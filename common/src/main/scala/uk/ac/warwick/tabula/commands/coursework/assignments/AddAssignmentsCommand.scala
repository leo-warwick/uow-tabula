package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.{DateTime, LocalDate}
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{BindingResult, Errors, ValidationUtils}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.helpers.{LazyLists, LazyMaps, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, DateFormats, PermissionDeniedException}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.MapWrapper
import scala.collection.mutable

object AddAssignmentsCommand {
  def apply(department: Department, user: CurrentUser) =
    new AddAssignmentsCommandInternal(department, user)
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with ComposableCommand[Seq[Assignment]]
      with PopulatesAddAssignmentsCommand
      with AddAssignmentsCommandOnBind
      with AddAssignmentsValidation
      with AddAssignmentsDescription
      with AddAssignmentsPermissions
      with AddAssignmentsCommandState
      with AddAssignmentsCommandTriggers
      with AddAssignmentsCommandNotifications
}

/**
  * Sub-object on the form for binding each upstream assignment and some other properties.
  */
class AssignmentItem(
  // whether to create an assignment from this item or not
  var include: Boolean,
  var occurrence: String,
  var upstreamAssignment: AssessmentComponent
) {

  def this() = this(true, null, null)

  var assignmentService: AssessmentService = Wire.auto[AssessmentService]

  // set after bind
  var assessmentGroup: Option[UpstreamAssessmentGroup] = _

  // Name for new assignment. Defaults to the name of the upstream assignment, if provided.
  var name: String = Option(upstreamAssignment).map(_.name).orNull
  if (upstreamAssignment != null) upstreamAssignment.name else null

  // Will reference a key of AddAssignmentsCommand.optionsMap. In this way, many AssignmentItems
  // can share the same set of options without having to post many copies separately.
  var optionsId: String = _

  @WithinYears(maxPast = 3, maxFuture = 3)
  var openDate: DateTime = _

  @WithinYears(maxPast = 3, maxFuture = 3)
  var closeDate: DateTime = _

  var openEnded: JBoolean = false

  def sameAssignment(other: AssignmentItem): Boolean =
    upstreamAssignment == other.upstreamAssignment &&
      occurrence == other.occurrence
}


class AddAssignmentsCommandInternal(val department: Department, val user: CurrentUser) extends CommandInternal[Seq[Assignment]] {

  self: AddAssignmentsCommandState with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent with AssessmentMembershipServiceComponent =>

  override def applyInternal(): mutable.Buffer[Assignment] = {
    assignmentItems.asScala.filter(_.include).map(item => {
      val assignment = new Assignment()
      assignment.assignmentService = assessmentService
      assignment.addDefaultFields()
      assignment.academicYear = academicYear
      assignment.name = item.name

      assignment.module = findModule(item.upstreamAssignment).get

      assignment.openDate = item.openDate
      assignment.closeDate = item.closeDate

      // validation should have verified that there is an options set for us to use
      val options = optionsMap.get(item.optionsId)
      options.copySharedTo(assignment)

      // Do open-ended afterwards; it's a date item that we're copying, not from shared options
      assignment.openEnded = item.openEnded

      assessmentService.save(assignment)

      val assessmentGroup = new AssessmentGroup
      assessmentGroup.occurrence = item.occurrence
      assessmentGroup.assessmentComponent = item.upstreamAssignment
      assessmentGroup.assignment = assignment
      assessmentMembershipService.save(assessmentGroup)

      assignment.assessmentGroups.add(assessmentGroup)
      assessmentService.save(assignment)
      assignment
    })
  }

  private def findModule(upstreamAssignment: AssessmentComponent): Option[Module] = {
    val moduleCode = upstreamAssignment.moduleCodeBasic.toLowerCase
    moduleAndDepartmentService.getModuleByCode(moduleCode)
  }

}

trait PopulatesAddAssignmentsCommand extends PopulateOnForm {

  self: AddAssignmentsCommandState with AssessmentMembershipServiceComponent =>

  override def populate(): Unit = {
    assignmentItems.clear()
    if (academicYear != null) {
      assignmentItems.addAll(fetchAssignmentItems())
    }
  }

  /**
    * Determines whether this component should have its checkbox checked
    * by default when first loading up the list of assignments. We exclude
    * any items that most people probably won't want to import, but they
    * can alter this choice before continuing.
    */
  private def shouldIncludeByDefault(component: AssessmentComponent) =
    component.assessmentType == AssessmentType.Assignment &&
      component.assessmentGroup != "AO"

  private def fetchAssignmentItems(): JList[AssignmentItem] = {
    for {
      upstreamAssignment <- assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments)
      assessmentGroup <- assessmentMembershipService.getUpstreamAssessmentGroups(upstreamAssignment, academicYear).sortBy(_.occurrence)
    } yield {
      val item = new AssignmentItem(
        include = shouldIncludeByDefault(upstreamAssignment),
        occurrence = assessmentGroup.occurrence,
        upstreamAssignment = upstreamAssignment)
      item.assessmentGroup = Some(assessmentGroup)
      item
    }
  }.asJava
}

trait AddAssignmentsCommandOnBind extends BindListener {

  self: AddAssignmentsCommandState with AssessmentMembershipServiceComponent =>

  override def onBind(result: BindingResult): Unit = {
    // re-attach UpstreamAssessmentGroup objects based on the other properties
    for (item <- assignmentItems.asScala if item.assessmentGroup == null) {
      item.assessmentGroup = assessmentMembershipService.getUpstreamAssessmentGroup(new UpstreamAssessmentGroup {
        this.academicYear = academicYear
        this.occurrence = item.occurrence
        this.moduleCode = item.upstreamAssignment.moduleCode
        this.sequence = item.upstreamAssignment.sequence
        this.assessmentGroup = item.upstreamAssignment.assessmentGroup
      })
    }
  }
}

trait AddAssignmentsValidation extends SelfValidating with Logging {

  self: AddAssignmentsCommandState with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent =>

  override def validate(errors: Errors) {
    ValidationUtils.rejectIfEmpty(errors, "academicYear", "NotEmpty")

    // just get the items we're actually going to import
    val items = includedItems
    val definedOptionsIds = optionsMap.keySet

    def missingOptionId(item: AssignmentItem) = {
      !definedOptionsIds.contains(item.optionsId)
    }

    def missingDates(item: AssignmentItem) = {
      item.openDate == null || (!item.openEnded && item.closeDate == null)
    }

    // reject if any items have a missing or garbage optionId value
    if (items.exists(missingOptionId)) {
      errors.reject("assignmentItems.missingOptions")
    }

    // reject if any items are missing date values
    if (items.exists(missingDates)) {
      errors.reject("assignmentItems.missingDates")
    }

    validateNames(errors)

    if (!errors.hasErrors) checkPermissions()
  }

  def validateNames(errors: Errors) {
    val items = includedItems
    val modules = LazyMaps.create { (code: String) => moduleAndDepartmentService.getModuleByCode(code.toLowerCase).orNull }

    for (item <- items) {
      for (existingAssignment <- assessmentService.getAssignmentByNameYearModule(item.name, academicYear, modules(item.upstreamAssignment.moduleCodeBasic))) {
        val path = "assignmentItems[%d]" format assignmentItems.indexOf(item)
        errors.rejectValue(path, "name.duplicate.assignment", Array(item.name), null)
      }

      def sameNameAs(item: AssignmentItem)(other: AssignmentItem) = {
        other != item && other.name == item.name
      }

      // also check that the upstream assignment names don't collide within a module.
      // group items by module, then look for duplicates within each group.
      val groupedByModule = items.groupBy(_.upstreamAssignment.moduleCodeBasic)
      for ((modCode, moduleItems) <- groupedByModule;
           item <- moduleItems
           if moduleItems.exists(sameNameAs(item))) {

        val path = "assignmentItems[%d]" format assignmentItems.indexOf(item)
        // Can't work out why it will end up trying to add the same error multiple times,
        // so wrapping in hasFieldErrors to limit it to showing just the first
        if (!errors.hasFieldErrors(path)) {
          errors.rejectValue(path, "name.duplicate.assignment.upstream", item.name)
        }

      }
    }
  }

  private def checkPermissions() = {
    // check that all the selected items are part of this department. Otherwise you could post the IDs of
    // unrelated assignments and do stuff with them.
    // Use .exists() to see if there is at least one with a matching department code OR parent department code
    def modules(d: Department): Seq[Module] = d.modules.asScala

    def modulesIncludingSubDepartments(d: Department): Seq[Module] =
      modules(d) ++ d.children.asScala.flatMap(modulesIncludingSubDepartments)

    val deptModules =
      if (includeSubDepartments) modulesIncludingSubDepartments(department)
      else modules(department)

    val hasInvalidAssignments = assignmentItems.asScala.exists { (item) =>
      !deptModules.contains(item.upstreamAssignment.module)
    }

    if (hasInvalidAssignments) {
      logger.warn("Rejected request to setup assignments that aren't in this department")
      throw PermissionDeniedException(user, Permissions.Assignment.ImportFromExternalSystem, department)
    }
  }

}

trait AddAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: AddAssignmentsCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.Assignment.ImportFromExternalSystem, department)
  }

}

trait AddAssignmentsDescription extends Describable[Seq[Assignment]] {

  self: AddAssignmentsCommandState =>

  override lazy val eventName = "AddAssignments"

  override def describe(d: Description) {
    d.department(department)
  }
}

trait AddAssignmentsCommandState {
  def department: Department

  def user: CurrentUser

  // academic year to create all these assignments under. Defaults to whatever academic year it will be in 3
  // months, which means it will start defaulting to next year from about May (under the assumption that
  // you would've done the current year's import long before then).
  var academicYear: AcademicYear = AcademicYear.forDate(LocalDate.now.plusMonths(3))
  var includeSubDepartments: Boolean = false

  // All the possible assignments, prepopulated from SITS.
  var assignmentItems: JList[AssignmentItem] = LazyLists.create()

  protected def includedItems: mutable.Buffer[AssignmentItem] = assignmentItems.asScala.filter(_.include)

  /**
    * options which are referenced by key by AssignmentItem.optionsId
    */
  var optionsMap: JMap[String, SharedAssignmentPropertiesForm] =
    new MapWrapper(LazyMaps.create { key: String => new SharedAssignmentPropertiesForm })

  val DEFAULT_OPEN_HOUR = 12
  val DEFAULT_WEEKS_LENGTH = 4

  // just for prepopulating the date form fields.
  @WithinYears(maxPast = 3, maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  @BeanProperty
  val defaultOpenDate: DateTime = new DateTime().withTime(DEFAULT_OPEN_HOUR, 0, 0, 0)

  @WithinYears(maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  @BeanProperty
  val defaultCloseDate: DateTime = defaultOpenDate.plusWeeks(DEFAULT_WEEKS_LENGTH)

  @BeanProperty
  val defaultOpenEnded = false
}

trait AddAssignmentsCommandTriggers extends GeneratesTriggers[Seq[Assignment]] {

  def generateTriggers(assignments: Seq[Assignment]): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    assignments.filter(assignment => assignment.closeDate != null && assignment.closeDate.isAfterNow).map(assignment =>
      AssignmentClosedTrigger(assignment.closeDate, assignment)
    )
  }
}

trait AddAssignmentsCommandNotifications extends SchedulesNotifications[Seq[Assignment], Assignment] with SharedAssignmentCommandNotifications {

  override def transformResult(assignments: Seq[Assignment]): Seq[Assignment] = assignments

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = generateScheduledNotifications(assignment)

}
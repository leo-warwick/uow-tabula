package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.format.{DateTimeFormat => JodaDateTimeFormat}
import org.joda.time.{DateTime, DateTimeConstants, LocalDate, LocalTime}
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{BindingResult, Errors, ValidationUtils}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.helpers.{LazyLists, LazyMaps, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.{DateWithinYears, WithinYears}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, DateFormats, PermissionDeniedException}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

object AddSitsAssignmentsCommand {
  def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
    new AddSitsAssignmentsCommandInternal(department, academicYear, user)
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with ComposableCommand[Seq[Assignment]]
      with PopulatesAddSitsAssignmentsCommand
      with AddSitsAssignmentsCommandOnBind
      with AddSitsAssignmentsValidation
      with AddSitsAssignmentsDescription
      with AddSitsAssignmentsPermissions
      with AddSitsAssignmentsCommandState
      with AddSitsAssignmentsCommandTriggers
      with AddSitsAssignmentsCommandNotifications
}

/**
  * Sub-object on the form for binding each upstream assignment and some other properties.
  */
class SitsAssignmentItem(
  // whether to create an assignment from this item or not
  var include: Boolean,
  var occurrence: String,
  var assessmentComponent: AssessmentComponent
) {

  def this() = this(true, null, null)

  var assignmentService: AssessmentService = Wire.auto[AssessmentService]

  // set after bind
  var assessmentGroup: Option[UpstreamAssessmentGroup] = _

  // Name for new assignment. Defaults to the name of the upstream assignment, if provided.
  var name: String = Option(assessmentComponent).map(_.name).orNull
  if (assessmentComponent != null) assessmentComponent.name else null

  // Will reference a key of AddSitsAssignmentsCommand.optionsMap. In this way, many SitsAssignmentItems
  // can share the same set of options without having to post many copies separately.
  var optionsId: String = _

  @DateWithinYears(maxPast = 3, maxFuture = 3)
  var openDate: LocalDate = _

  @WithinYears(maxPast = 3, maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var closeDate: DateTime = _

  var openEnded: JBoolean = false

  def sameAssignment(other: SitsAssignmentItem): Boolean =
    assessmentComponent == other.assessmentComponent &&
      occurrence == other.occurrence
}


class AddSitsAssignmentsCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser) extends CommandInternal[Seq[Assignment]] {

  self: AddSitsAssignmentsCommandState with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Seq[Assignment] = {
    sitsAssignmentItems.asScala.toSeq.filter(_.include).map(item => {
      val assignment = new Assignment()
      assignment.assignmentService = assessmentService
      assignment.addDefaultFields()
      assignment.academicYear = academicYear
      assignment.name = item.name

      assignment.module = findModule(item.assessmentComponent).get

      assignment.openDate = item.openDate.toDateTime(Assignment.openTime)
      if(!item.openEnded) {
        assignment.closeDate = item.closeDate
      }
      assignment.workflowCategory = Some(WorkflowCategory.NotDecided)

      // validation should have verified that there is an options set for us to use
      val options = optionsMap.get(item.optionsId)
      options.copySharedTo(assignment)

      // Do open-ended afterwards; it's a date item that we're copying, not from shared options
      assignment.openEnded = item.openEnded

      assessmentService.save(assignment)

      val assessmentGroup = new AssessmentGroup
      assessmentGroup.occurrence = item.occurrence
      assessmentGroup.assessmentComponent = item.assessmentComponent
      assessmentGroup.assignment = assignment
      assessmentMembershipService.save(assessmentGroup)

      assignment.assessmentGroups.add(assessmentGroup)
      assessmentService.save(assignment)
      assignment
    })
  }

  private def findModule(assessmentComponent: AssessmentComponent): Option[Module] = {
    val moduleCode = assessmentComponent.moduleCodeBasic.toLowerCase
    moduleAndDepartmentService.getModuleByCode(moduleCode)
  }

}

trait PopulatesAddSitsAssignmentsCommand extends PopulateOnForm {

  self: AddSitsAssignmentsCommandState with AssessmentMembershipServiceComponent =>

  override def populate(): Unit = {
    sitsAssignmentItems.clear()
    sitsAssignmentItems.addAll(fetchSitsAssignmentItems())
  }

  /**
    * Determines whether this component should have its checkbox checked
    * by default when first loading up the list of assignments. We exclude
    * any items that most people probably won't want to import, but they
    * can alter this choice before continuing.
    */
  private def shouldIncludeByDefault(component: AssessmentComponent) =
    component.assessmentType.subtype == TabulaAssessmentSubtype.Assignment &&
      component.assessmentGroup != "AO"

  private def fetchSitsAssignmentItems(): JList[SitsAssignmentItem] = {
    for {
      assessmentComponent <- assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments, inUseOnly = true)
      assessmentGroup <- assessmentMembershipService.getUpstreamAssessmentGroups(assessmentComponent, academicYear).sortBy(_.occurrence)
    } yield {
      val item = new SitsAssignmentItem(
        include = shouldIncludeByDefault(assessmentComponent),
        occurrence = assessmentGroup.occurrence,
        assessmentComponent = assessmentComponent)
      item.assessmentGroup = Some(assessmentGroup)
      item
    }
  }.asJava
}

trait AddSitsAssignmentsCommandOnBind extends BindListener {

  self: AddSitsAssignmentsCommandState with AssessmentMembershipServiceComponent =>

  override def onBind(result: BindingResult): Unit = {
    // re-attach UpstreamAssessmentGroup objects based on the other properties
    for (item <- sitsAssignmentItems.asScala if item.assessmentGroup == null) {
      val year = academicYear // Allow to break inner scope
      item.assessmentGroup = assessmentMembershipService.getUpstreamAssessmentGroup(new UpstreamAssessmentGroup {
        this.academicYear = year
        this.occurrence = item.occurrence
        this.moduleCode = item.assessmentComponent.moduleCode
        this.sequence = item.assessmentComponent.sequence
        this.assessmentGroup = item.assessmentComponent.assessmentGroup
      }, eagerLoad = false)
    }
  }
}

trait AddSitsAssignmentsValidation extends SelfValidating with Logging {

  self: AddSitsAssignmentsCommandState with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent =>

  override def validate(errors: Errors): Unit = {
    ValidationUtils.rejectIfEmpty(errors, "academicYear", "NotEmpty")

    // just get the items we're actually going to import
    val items = includedItems
    val definedOptionsIds = optionsMap.keySet

    def missingOptionId(item: SitsAssignmentItem) = {
      !definedOptionsIds.contains(item.optionsId)
    }

    def missingDates(item: SitsAssignmentItem) = {
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
    validateDates(errors)

    if (!errors.hasErrors) checkPermissions()
  }

  def validateNames(errors: Errors): Unit = {
    val items = includedItems
    val modules = LazyMaps.create { (code: String) => moduleAndDepartmentService.getModuleByCode(code.toLowerCase).orNull }

    for (item <- items) {
      for (existingAssignment <- assessmentService.getAssignmentByNameYearModule(item.name, academicYear, modules(item.assessmentComponent.moduleCodeBasic))) {
        val path = "sitsAssignmentItems[%d]".format(sitsAssignmentItems.indexOf(item))
        errors.rejectValue(path, "name.duplicate.assignment", Array(item.name, academicYear.toString), null)
      }

      def sameNameAs(item: SitsAssignmentItem)(other: SitsAssignmentItem) = {
        other != item && other.name == item.name
      }

      // also check that the upstream assignment names don't collide within a module.
      // group items by module, then look for duplicates within each group.
      val groupedByModule = items.groupBy(_.assessmentComponent.moduleCodeBasic)
      for ((modCode, moduleItems) <- groupedByModule;
           item <- moduleItems
           if moduleItems.exists(sameNameAs(item))) {

        val path = "sitsAssignmentItems[%d]".format(sitsAssignmentItems.indexOf(item))
        // Can't work out why it will end up trying to add the same error multiple times,
        // so wrapping in hasFieldErrors to limit it to showing just the first
        if (!errors.hasFieldErrors(path)) {
          errors.rejectValue(path, "name.duplicate.assignment.upstream", item.name)
        }

      }
    }
  }

  private[this] lazy val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.map(_.asJoda).sorted

  def validateDates(errors: Errors): Unit =
      for (item <- includedItems) {
      val path = "sitsAssignmentItems[%d]".format(sitsAssignmentItems.indexOf(item))

      if (item.openDate != null) {
        if (holidayDates.contains(item.openDate) || item.openDate.getDayOfWeek == DateTimeConstants.SATURDAY || item.openDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
          errors.rejectValue(path, "openDate.notWorkingDay")
        }
      }

      if (item.closeDate != null && !item.openEnded) {
        if (holidayDates.contains(item.closeDate.toLocalDate) || item.closeDate.getDayOfWeek == DateTimeConstants.SATURDAY || item.closeDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
          errors.rejectValue(path, "closeDate.notWorkingDay")
        }
        if(!Assignment.isValidCloseTime(item.closeDate)) {
          val formatter = JodaDateTimeFormat.forPattern("ha")
          val times: Array[AnyRef] = Array(formatter.print(Assignment.CloseTimeStart).toLowerCase, formatter.print(Assignment.CloseTimeEnd).toLowerCase)
          errors.rejectValue(path, "closeDate.invalidTime", times, "")
        }
      }
    }

  private def checkPermissions() = {
    // check that all the selected items are part of this department. Otherwise you could post the IDs of
    // unrelated assignments and do stuff with them.
    // Use .exists() to see if there is at least one with a matching department code OR parent department code
    def modules(d: Department): Seq[Module] = d.modules.asScala.toSeq

    def modulesIncludingSubDepartments(d: Department): Seq[Module] =
      modules(d) ++ d.children.asScala.flatMap(modulesIncludingSubDepartments)

    val deptModules =
      if (includeSubDepartments) modulesIncludingSubDepartments(department)
      else modules(department)

    val hasInvalidAssignments = sitsAssignmentItems.asScala.exists { (item) =>
      !deptModules.contains(item.assessmentComponent.module)
    }

    if (hasInvalidAssignments) {
      logger.warn("Rejected request to setup assignments that aren't in this department")
      throw PermissionDeniedException(user, Permissions.Assignment.ImportFromExternalSystem, department)
    }
  }

}

trait AddSitsAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: AddSitsAssignmentsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Assignment.ImportFromExternalSystem, department)
  }

}

trait AddSitsAssignmentsDescription extends Describable[Seq[Assignment]] {

  self: AddSitsAssignmentsCommandState =>

  override lazy val eventName = "AddSitsAssignments"

  override def describe(d: Description): Unit = {
    d.department(department)
  }
}

trait AddSitsAssignmentsCommandState {
  def department: Department

  def academicYear: AcademicYear

  def user: CurrentUser

  var includeSubDepartments: Boolean = false

  // All the possible assignments, prepopulated from SITS.
  var sitsAssignmentItems: JList[SitsAssignmentItem] = LazyLists.create[SitsAssignmentItem]()

  def includedItems: Seq[SitsAssignmentItem] = sitsAssignmentItems.asScala.toSeq.filter(_.include)

  /**
    * options which are referenced by key by SitsAssignmentItem.optionsId
    */
  var optionsMap: JMap[String, SharedAssignmentPropertiesForm] =
    LazyMaps.create { key: String => new SharedAssignmentPropertiesForm }.asJava

  val DEFAULT_OPEN_HOUR = 12
  val DEFAULT_WEEKS_LENGTH = 4

  // just for prepopulating the date form fields.
  @DateWithinYears(maxPast = 3, maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DatePickerPattern)
  @BeanProperty
  val defaultOpenDate: LocalDate = LocalDate.now()

  @WithinYears(maxPast = 3, maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  @BeanProperty
  val defaultCloseDate: DateTime = defaultOpenDate.plusWeeks(DEFAULT_WEEKS_LENGTH).toDateTime(new LocalTime(12, 0))

  @BeanProperty
  val defaultOpenEnded = false
}

trait AddSitsAssignmentsCommandTriggers extends GeneratesTriggers[Seq[Assignment]] {

  def generateTriggers(assignments: Seq[Assignment]): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    assignments.filter(assignment => assignment.closeDate != null && assignment.closeDate.isAfterNow).map(assignment =>
      AssignmentClosedTrigger(assignment.closeDate, assignment)
    )
  }
}

trait AddSitsAssignmentsCommandNotifications extends SchedulesNotifications[Seq[Assignment], Assignment] with GeneratesNotificationsForAssignment {

  override def transformResult(assignments: Seq[Assignment]): Seq[Assignment] = assignments

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = generateNotifications(assignment)

}


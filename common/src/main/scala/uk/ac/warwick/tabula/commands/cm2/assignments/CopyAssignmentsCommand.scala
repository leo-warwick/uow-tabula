package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.{DateTimeConstants, Duration, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.CopyAssignmentsCommand._
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{CopyMarkingWorkflowCommandComponent, CopyMarkingWorkflowComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.jdk.CollectionConverters._

object CopyAssignmentsCommand {
  type Result = Seq[Assignment]
  type Command = Appliable[Result] with CopyAssignmentsState

  val AdminPermission: Permission = Permissions.Assignment.Create

  def apply(department: Department, academicYear: AcademicYear): Command =
    new CopyDepartmentAssignmentsCommandInternal(department, academicYear)
      with ComposableCommand[Seq[Assignment]]
      with CopyDepartmentAssignmentsPermissions
      with CopyAssignmentsDescription
      with CopyAssignmentsCommandTriggers
      with CopyAssignmentsCommandNotifications
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with CopyMarkingWorkflowCommandComponent

  def apply(module: Module, academicYear: AcademicYear): Command =
    new CopyModuleAssignmentsCommandInternal(module, academicYear)
      with ComposableCommand[Seq[Assignment]]
      with CopyModuleAssignmentsPermissions
      with CopyAssignmentsDescription
      with CopyAssignmentsCommandTriggers
      with CopyAssignmentsCommandNotifications
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with CopyMarkingWorkflowCommandComponent
}

abstract class AbstractCopyAssignmentsCommandInternal
  extends CommandInternal[Result] with CopyAssignmentsState {
  self: AssessmentServiceComponent with AssessmentMembershipServiceComponent with CopyMarkingWorkflowComponent =>

  override def applyInternal(): Result = {
    assignments.asScala.toSeq.map { assignment =>
      val newAssignment = copy(assignment)
      assessmentService.save(newAssignment)
      newAssignment
    }
  }

  private[this] lazy val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.map(_.asJoda).sorted

  def copy(assignment: Assignment): Assignment = {
    val newAssignment = new Assignment()
    newAssignment.assignmentService = assignment.assignmentService // FIXME Used in testing
    newAssignment.academicYear = academicYear

    // best guess of new open and close dates. likely to be wrong by up to a few weeks but better than out by years
    val yearOffest = academicYear.startYear - assignment.academicYear.startYear
    newAssignment.openDate = assignment.openDate.plusYears(yearOffest).withDayOfWeek(assignment.openDate.getDayOfWeek).withTime(Assignment.openTime)

    while (holidayDates.contains(newAssignment.openDate.toLocalDate) || newAssignment.openDate.getDayOfWeek == DateTimeConstants.SATURDAY || newAssignment.openDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
      newAssignment.openDate = newAssignment.openDate.plusDays(1)
    }

    if (assignment.openEnded) {
      newAssignment.closeDate = null
    } else {
      val timeComponent = if(Assignment.isValidCloseTime(assignment.closeDate)) assignment.closeDate.toLocalTime else Assignment.defaultCloseTime
      newAssignment.closeDate = newAssignment.openDate.plus(new Duration(assignment.openDate, assignment.closeDate)).withTime(timeComponent)

      while (holidayDates.contains(newAssignment.closeDate.toLocalDate) || newAssignment.closeDate.getDayOfWeek == DateTimeConstants.SATURDAY || newAssignment.closeDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
        newAssignment.closeDate = newAssignment.closeDate.plusDays(1)
      }
    }

    // copy the other fields from the target assignment
    newAssignment.module = assignment.module
    newAssignment.name = assignment.name
    newAssignment.openEnded = assignment.openEnded
    newAssignment.resitAssessment = assignment.resitAssessment
    newAssignment.collectMarks = assignment.collectMarks
    newAssignment.collectSubmissions = assignment.collectSubmissions
    newAssignment.restrictSubmissions = assignment.restrictSubmissions
    newAssignment.allowLateSubmissions = assignment.allowLateSubmissions
    newAssignment.allowResubmission = assignment.allowResubmission
    newAssignment.displayPlagiarismNotice = assignment.displayPlagiarismNotice
    newAssignment.allowExtensions = assignment.allowExtensions
    newAssignment.extensionAttachmentMandatory = assignment.extensionAttachmentMandatory
    newAssignment.allowExtensionsAfterCloseDate = assignment.allowExtensionsAfterCloseDate
    newAssignment.summative = assignment.summative
    newAssignment.dissertation = assignment.dissertation
    newAssignment.publishFeedback = assignment.publishFeedback
    newAssignment.feedbackTemplate = assignment.feedbackTemplate
    newAssignment.includeInFeedbackReportWithoutSubmissions = assignment.includeInFeedbackReportWithoutSubmissions
    newAssignment.automaticallyReleaseToMarkers = assignment.automaticallyReleaseToMarkers
    newAssignment.automaticallySubmitToTurnitin = assignment.automaticallySubmitToTurnitin
    newAssignment.turnitinStoreInRepository = assignment.turnitinStoreInRepository
    newAssignment.turnitinExcludeBibliography = assignment.turnitinExcludeBibliography
    newAssignment.turnitinExcludeQuoted = assignment.turnitinExcludeQuoted
    newAssignment.anonymity = assignment._anonymity
    newAssignment.cm2MarkingWorkflow = assignment.cm2MarkingWorkflow match {
      // None
      case null => null

      // Re-usable
      case workflow: CM2MarkingWorkflow if workflow.isReusable => workflow

      // Single-use
      case workflow: CM2MarkingWorkflow => copyMarkingWorkflow(assignment.module.adminDepartment, workflow)
    }
    // don't copy createdByAEP property
    newAssignment.createdByAEP = false

    var workflowCtg = assignment.workflowCategory match {
      case Some(workflowCategory: WorkflowCategory) =>
        Some(workflowCategory)
      case _ =>
        Some(WorkflowCategory.NotDecided)
    }
    newAssignment.workflowCategory = workflowCtg

    newAssignment.addDefaultFields()

    newAssignment.addFields(assignment.fields.asScala.toSeq.sortBy(_.position).map(field => {
      newAssignment.findField(field.name).foreach(newAssignment.removeField)
      field.duplicate(newAssignment)
    }): _*)

    // TAB-1175 Guess SITS links
    assignment.assessmentGroups.asScala
      // Only where defined in the new year
      .filter(_.toUpstreamAssessmentGroupInfo(newAssignment.academicYear).nonEmpty)
      .foreach { group =>
        val newGroup = new AssessmentGroup
        newGroup.assessmentComponent = group.assessmentComponent
        newGroup.occurrence = group.occurrence
        newGroup.assignment = newAssignment
        newAssignment.assessmentGroups.add(newGroup)
      }

    newAssignment
  }
}

class CopyDepartmentAssignmentsCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends AbstractCopyAssignmentsCommandInternal with CopyDepartmentAssignmentsState {
  self: AssessmentServiceComponent with AssessmentMembershipServiceComponent with CopyMarkingWorkflowComponent =>
}

class CopyModuleAssignmentsCommandInternal(val module: Module, val academicYear: AcademicYear)
  extends AbstractCopyAssignmentsCommandInternal with CopyModuleAssignmentsState {
  self: AssessmentServiceComponent with AssessmentMembershipServiceComponent with CopyMarkingWorkflowComponent =>
}

trait CopyDepartmentAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CopyDepartmentAssignmentsState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(department))
}

trait CopyModuleAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CopyModuleAssignmentsState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(module))
}

trait CopyAssignmentsState {
  def modules: Seq[Module]

  def academicYear: AcademicYear

  var assignments: JList[Assignment] = JArrayList()
}

trait CopyDepartmentAssignmentsState extends CopyAssignmentsState {
  def department: Department

  def modules: Seq[Module] = department.modules.asScala.toSeq.filter(_.assignments.asScala.exists(_.isAlive)).sortBy(_.code)
}

trait CopyModuleAssignmentsState extends CopyAssignmentsState {
  def module: Module

  def modules: Seq[Module] = Seq(module)
}

trait CopyAssignmentsDescription extends Describable[Result] {
  self: CopyAssignmentsState =>

  override lazy val eventName = "CopyAssignmentsFromPrevious"

  def describe(d: Description): Unit = d
    .properties("modules" -> modules.map(_.id))
    .properties("assignments" -> assignments.asScala.map(_.id))
}

trait CopyAssignmentsCommandTriggers extends GeneratesTriggers[Result] {

  def generateTriggers(assignments: Seq[Assignment]): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    assignments.filter(assignment => assignment.closeDate != null && assignment.closeDate.isAfterNow).map(assignment =>
      AssignmentClosedTrigger(assignment.closeDate, assignment)
    )
  }
}

trait CopyAssignmentsCommandNotifications extends SchedulesNotifications[Result, Assignment] with GeneratesNotificationsForAssignment {

  override def transformResult(assignments: Seq[Assignment]): Seq[Assignment] = assignments

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = generateNotifications(assignment)

}


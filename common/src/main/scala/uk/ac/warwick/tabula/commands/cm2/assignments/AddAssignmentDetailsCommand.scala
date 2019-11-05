package uk.ac.warwick.tabula.commands.cm2.assignments

import org.hibernate.validator.constraints.{Length, NotEmpty}
import org.joda.time.{DateTimeConstants, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{CreatesMarkingWorkflow, ModifyMarkingWorkflowState, ModifyMarkingWorkflowValidation}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.jdk.CollectionConverters._

object CreateAssignmentDetailsCommand {
  def apply(module: Module, academicYear: AcademicYear) =
    new CreateAssignmentDetailsCommandInternal(module, academicYear)
      with ComposableCommand[Assignment]
      with BooleanAssignmentDetailProperties
      with CreateAssignmentPermissions
      with CreateAssignmentDetailsDescription
      with CreateAssignmentDetailsCommandState
      with CreateAssignmentDetailsValidation
      with ModifyAssignmentScheduledNotifications
      with AutowiringAssessmentServiceComponent
      with ModifyAssignmentsDetailsTriggers
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
}

class CreateAssignmentDetailsCommandInternal(val module: Module, val academicYear: AcademicYear) extends CommandInternal[Assignment]
  with CreateAssignmentDetailsCommandState with SharedAssignmentDetailProperties with AssignmentDetailsCopy with CreatesMarkingWorkflow {
  self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent =>

  private var _prefilled: Boolean = _

  def prefilled: Boolean = _prefilled

  markersA = JArrayList()
  markersB = JArrayList()

  override def applyInternal(): Assignment = {
    val assignment = new Assignment(module)
    // Set default booleans
    BooleanAssignmentProperties(assignment)

    assignment.addDefaultFields()
    copyTo(assignment)
    if (workflowCategory == WorkflowCategory.SingleUse) {
      createAndSaveSingleUseWorkflow(assignment)
    }
    assessmentService.save(assignment)
    assignment
  }


  def prefillFromRecentAssignment() {
    if (prefillAssignment != null) {
      copyNonspecificFrom(prefillAssignment)
    } else {
      if (prefillFromRecent) {
        for (a <- assessmentService.recentAssignment(module.adminDepartment)) {
          copyNonspecificFrom(a)
          _prefilled = true
        }
      }
    }
  }

  /**
    * Copy just the fields that it might be useful to
    * prefill. The assignment passed in might typically be
    * another recently created assignment, that may have good
    * initial values for submission options.
    */
  def copyNonspecificFrom(assignment: Assignment) {
    openDate = Option(assignment.openDate).map(_.toLocalDate).orNull
    closeDate = Option(assignment.closeDate).map(_.toLocalDate).orNull
    workflowCategory = assignment.workflowCategory.getOrElse(WorkflowCategory.NotDecided)
    if (assignment.workflowCategory.contains(WorkflowCategory.Reusable)) {
      reusableWorkflow = assignment.cm2MarkingWorkflow
    }
    copySharedDetailFrom(assignment)
  }

}

trait AssignmentDetailsCopy extends ModifyAssignmentDetailsCommandState with SharedAssignmentDetailProperties {
  self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent with ModifyMarkingWorkflowState =>

  def copyTo(assignment: Assignment) {
    assignment.name = name

    if (assignment.openDate == null || !openDate.isEqual(assignment.openDate.toLocalDate)) {
      assignment.openDate = openDate.toDateTime(Assignment.openTime)
    }

    assignment.academicYear = academicYear
    if (openEnded) {
      assignment.openEndedReminderDate = Option(openEndedReminderDate).map(_.toDateTime(Assignment.openTime)).orNull
      assignment.closeDate = null
    } else {
      assignment.openEndedReminderDate = null

      if (assignment.closeDate == null || !closeDate.isEqual(assignment.closeDate.toLocalDate)) {
        assignment.closeDate = closeDate.toDateTime(Assignment.closeTime)
      }
    }

    assignment.workflowCategory = Some(workflowCategory)
    if (workflowCategory == WorkflowCategory.Reusable) {
      assignment.cm2MarkingWorkflow = reusableWorkflow
    }
    assignment.cm2Assignment = true
    assignment.anonymity = anonymity
    copySharedDetailTo(assignment)
  }
}

trait ModifyAssignmentDetailsCommandState {

  self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent with ModifyMarkingWorkflowState =>

  def module: Module

  def academicYear: AcademicYear

  @Length(max = 200)
  @NotEmpty(message = "{NotEmpty.assignmentName}")
  var name: String = _

  var openDate: LocalDate = LocalDate.now

  var closeDate: LocalDate = openDate.plusWeeks(2)

  var openEndedReminderDate: LocalDate = _

  var workflowCategory: WorkflowCategory = WorkflowCategory.NotDecided

  lazy val workflowCategories: Seq[WorkflowCategory] = {
    WorkflowCategory.values
  }

  var reusableWorkflow: CM2MarkingWorkflow = _

  lazy val department: Department = module.adminDepartment
  lazy val availableWorkflows: Seq[CM2MarkingWorkflow] =
    cm2MarkingWorkflowService.getReusableWorkflows(department, academicYear)

  var anonymity: AssignmentAnonymity = _

}

trait CreateAssignmentDetailsCommandState extends ModifyAssignmentDetailsCommandState with ModifyMarkingWorkflowState {
  self: AssessmentServiceComponent with UserLookupComponent with CM2MarkingWorkflowServiceComponent =>

  // can be set to false if that's not what you want.
  var prefillFromRecent = true
  var prefillAssignment: Assignment = _
}

trait ModifyAssignmentDetailsValidation extends SelfValidating with ModifyMarkingWorkflowValidation {
  self: ModifyAssignmentDetailsCommandState with BooleanAssignmentDetailProperties with AssessmentServiceComponent with ModifyMarkingWorkflowState
    with UserLookupComponent =>

  private[this] lazy val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.map(_.asJoda).sorted

  // validation shared between add and edit
  def genericValidate(errors: Errors): Unit = {
    if (openDate == null) {
      errors.rejectValue("openDate", "openDate.missing")
    }

    if (!openEnded) {
      if (closeDate == null) {
        errors.rejectValue("closeDate", "closeDate.missing")
      } else if (openDate != null && openDate.isAfter(closeDate)) {
        errors.rejectValue("closeDate", "closeDate.early")
      }
    }
    if (workflowCategory == WorkflowCategory.Reusable && reusableWorkflow == null) {
      errors.rejectValue("reusableWorkflow", "markingWorkflow.reusableWorkflow.none")
    } else if (workflowCategory == WorkflowCategory.SingleUse) {
      if (workflowType == null) {
        errors.rejectValue("workflowType", "markingWorkflow.workflowType.none")
      } else if ((workflowType.name == "DoubleBlind") && (markersA.size() <= 1)) {
        errors.rejectValue("markersA", "NotEnough.markersA", Array("two"), "")
      } else {
        markerValidation(errors, workflowType)
      }
    }
  }

  // Validation shared between add and edit but may be opt in (i.e. may not validate on edit if it hasn't changed)
  def validateOpenDate(errors: Errors): Unit = {
    if (openDate != null) {
      if (holidayDates.contains(openDate) || openDate.getDayOfWeek == DateTimeConstants.SATURDAY || openDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
        errors.rejectValue("openDate", "openDate.notWorkingDay")
      }
    }
  }

  // Validation shared between add and edit but may be opt in (i.e. may not validate on edit if it hasn't changed)
  def validateCloseDate(errors: Errors): Unit = {
    if (closeDate != null && !openEnded) {
      if (holidayDates.contains(closeDate) || closeDate.getDayOfWeek == DateTimeConstants.SATURDAY || closeDate.getDayOfWeek == DateTimeConstants.SUNDAY) {
        errors.rejectValue("closeDate", "closeDate.notWorkingDay")
      }
    }
  }
}


trait CreateAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation with ModifyMarkingWorkflowValidation {
  self: CreateAssignmentDetailsCommandState with BooleanAssignmentDetailProperties with AssessmentServiceComponent
    with ModifyMarkingWorkflowState with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    // TAB-255 Guard to avoid SQL error - if it's null or gigantic it will fail validation in other ways.
    if (name != null && name.length < 3000) {
      val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter(_.isAlive)
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "name.duplicate.assignment", Array(duplicate.name), "")
      }
    }

    genericValidate(errors)

    // We always validate open and close dates for new assignments
    validateOpenDate(errors)
    validateCloseDate(errors)
  }
}

trait CreateAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateAssignmentDetailsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Assignment.Create, module)
  }
}

trait CreateAssignmentDetailsDescription extends Describable[Assignment] {
  self: CreateAssignmentDetailsCommandState =>

  override lazy val eventName = "AddAssignmentDetails"

  override def describe(d: Description) {
    d.module(module).properties(
      "name" -> name,
      "openDate" -> Option(openDate).map(_.toString()).orNull,
      "closeDate" -> Option(closeDate).map(_.toString()).orNull,
      "workflowCtg" -> Option(workflowCategory).map(_.code).orNull,
      "workflowType" -> Option(workflowType).map(_.name).orNull,
      "anonymity" -> Option(anonymity).map(_.code).orNull
    )
  }

}

trait GeneratesNotificationsForAssignment {

  def generateNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = {
    // if the assignment doesn't collect submissions or is open ended then don't schedule any notifications about deadlines
    if (!assignment.collectSubmissions || assignment.openEnded) {
      Seq()
    } else {
      val dayOfDeadline = assignment.closeDate.withTime(0, 0, 0, 0)

      val submissionNotifications = {
        // skip the week late notification if late submission isn't possible
        val daysToSend = if (assignment.allowLateSubmissions) {
          Seq(-7, -1, 1, 7)
        } else {
          Seq(-7, -1, 1)
        }

        val surroundingTimes = for (day <- daysToSend) yield assignment.closeDate.plusDays(day)
        val proposedTimes = Seq(dayOfDeadline) ++ surroundingTimes

        // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
        val allTimes = proposedTimes.filter(_.isAfterNow)

        allTimes.map {
          when =>
            new ScheduledNotification[Assignment]("SubmissionDueGeneral", assignment, when)
        }
      }

      val feedbackDeadline = assignment.feedbackDeadline
      val feedbackNotifications =
        if (assignment.dissertation || !assignment.publishFeedback || feedbackDeadline.isEmpty) // No feedback deadline for dissertations or late submissions
          Seq()
        else {
          val daysToSend = Seq(-7, -1, 0)

          val proposedTimes = for (day <- daysToSend) yield feedbackDeadline.get
            .plusDays(day).toDateTimeAtStartOfDay

          // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
          val allTimes = proposedTimes.filter(_.isAfterNow)

          allTimes.map {
            when =>
              new ScheduledNotification[Assignment]("FeedbackDueGeneral", assignment, when)
          }
        }

      submissionNotifications ++ feedbackNotifications
    }
  }
}

trait ModifyAssignmentScheduledNotifications
  extends SchedulesNotifications[Assignment, Assignment] with GeneratesNotificationsForAssignment {

  override def transformResult(assignment: Assignment) = Seq(assignment)

  override def scheduledNotifications(assignment: Assignment): Seq[ScheduledNotification[Assignment]] = {
    generateNotifications(assignment)
  }

}

trait ModifyAssignmentsDetailsTriggers extends GeneratesTriggers[Assignment] {

  def generateTriggers(commandResult: Assignment): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    if (commandResult.closeDate != null && commandResult.closeDate.isAfterNow) {
      Seq(AssignmentClosedTrigger(commandResult.closeDate, commandResult))
    } else {
      Seq()
    }
  }
}

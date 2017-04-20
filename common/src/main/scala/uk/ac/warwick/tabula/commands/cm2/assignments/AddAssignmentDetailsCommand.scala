package uk.ac.warwick.tabula.commands.cm2.assignments

import org.hibernate.validator.constraints.{Length, NotEmpty}
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.triggers.{AssignmentClosedTrigger, Trigger}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object CreateAssignmentDetailsCommand {
  def apply(module: Module) =
    new CreateAssignmentDetailsCommandInternal(module)
      with ComposableCommand[Assignment]
      with BooleanAssignmentProperties
      with CreateAssignmentPermissions
      with CreateAssignmentDetailsDescription
      with CreateAssignmentDetailsCommandState
      with CreateAssignmentDetailsValidation
      with ModifyAssignmentScheduledNotifications
      with AutowiringAssessmentServiceComponent
      with ModifyAssignmentsDetailsTriggers
}

class CreateAssignmentDetailsCommandInternal(val module: Module)
  extends CommandInternal[Assignment] with CreateAssignmentDetailsCommandState with SharedAssignmentProperties with AssignmentDetailsCopy {

  self: AssessmentServiceComponent =>

  private var _prefilled: Boolean = _

  def prefilled: Boolean = _prefilled

  override def applyInternal(): Assignment = {
    val assignment = new Assignment(module)
    assignment.addDefaultFields()
    copyTo(assignment)
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
    openDate = assignment.openDate
    closeDate = assignment.closeDate
    copySharedFrom(assignment)
  }


}


trait AssignmentDetailsCopy extends ModifyAssignmentDetailsCommandState with SharedAssignmentProperties {
  self: AssessmentServiceComponent =>

  def copyTo(assignment: Assignment) {
    assignment.name = name
    assignment.openDate = openDate
    assignment.academicYear = academicYear
    if (openEnded) {
      assignment.openEndedReminderDate = openEndedReminderDate
      assignment.closeDate = null
    } else {
      assignment.openEndedReminderDate = null
      assignment.closeDate = closeDate
    }

    assignment.workflowCategory = Some(workflowCategory)
    copySharedTo(assignment: Assignment)
  }
}

trait ModifyAssignmentDetailsCommandState extends CurrentSITSAcademicYear {

  self: AssessmentServiceComponent =>


  @Length(max = 200)
  @NotEmpty(message = "{NotEmpty.assignmentName}")
  var name: String = _

  var openDate: DateTime = DateTime.now.withTime(0, 0, 0, 0)

  var closeDate: DateTime = openDate.plusWeeks(2).withTime(12, 0, 0, 0)

  var openEndedReminderDate: DateTime = _

  var workflowCategory: WorkflowCategory = WorkflowCategory.NotDecided

  lazy val workflowCategories: Seq[WorkflowCategory] = {
    WorkflowCategory.values
  }

}

trait CreateAssignmentDetailsCommandState extends ModifyAssignmentDetailsCommandState {
  self: AssessmentServiceComponent =>

  def module: Module

  // can be set to false if that's not what you want.
  var prefillFromRecent = true

  var prefillAssignment: Assignment = _

}

trait ModifyAssignmentDetailsValidation extends SelfValidating {
  self: ModifyAssignmentDetailsCommandState with BooleanAssignmentProperties with AssessmentServiceComponent =>

  // validation shared between add and edit
  def genericValidate(errors: Errors): Unit = {
    if (openDate == null) {
      errors.rejectValue("openDate", "openDate.missing")
    }

    if (!openEnded) {
      if (closeDate == null) {
        errors.rejectValue("closeDate", "closeDate.missing")
      } else if (openDate != null && openDate.isAfter(closeDate)) {
        errors.reject("closeDate.early")
      }
    }
  }
}


trait CreateAssignmentDetailsValidation extends ModifyAssignmentDetailsValidation {
  self: CreateAssignmentDetailsCommandState with BooleanAssignmentProperties with AssessmentServiceComponent =>

  override def validate(errors: Errors): Unit = {
    // TAB-255 Guard to avoid SQL error - if it's null or gigantic it will fail validation in other ways.
    if (name != null && name.length < 3000) {
      val duplicates = assessmentService.getAssignmentByNameYearModule(name, academicYear, module).filter { existing => existing.isAlive }
      for (duplicate <- duplicates.headOption) {
        errors.rejectValue("name", "name.duplicate.assignment", Array(name), "")
      }
    }
    genericValidate(errors)
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

  override def describe(d: Description) {
    d.module(module).properties(
      "name" -> name,
      "openDate" -> openDate,
      "closeDate" -> closeDate)
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
        if (assignment.dissertation || feedbackDeadline.isEmpty) // No feedback deadline for dissertations or late submissions
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

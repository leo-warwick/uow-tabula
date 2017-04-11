package uk.ac.warwick.tabula.commands.coursework.feedback

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.feedback.CheckSitsUploadCommand.Result
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, ExamFeedback, Feedback, FeedbackForSits}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsService, ExportFeedbackToSitsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringFeedbackForSitsServiceComponent, FeedbackForSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CheckSitsUploadCommand {

	case class Result(
		hasAssessmentGroups: Boolean = false,
		hasAssignmentRow: Boolean = false,
		hasWritableMark: Boolean = false,
		hasWritableGrade: Boolean = false
	)

	def apply(feedback: Feedback) =
		new CheckSitsUploadCommandInternal(feedback)
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringExportFeedbackToSitsServiceComponent
			with ComposableCommand[CheckSitsUploadCommand.Result]
			with CheckSitsUploadValidation
			with CheckSitsUploadPermissions
			with CheckSitsUploadCommandState
			with ReadOnly with Unaudited
}


class CheckSitsUploadCommandInternal(val feedback: Feedback) extends CommandInternal[CheckSitsUploadCommand.Result] {

	self: ExportFeedbackToSitsServiceComponent with CheckSitsUploadCommandState =>

	override def applyInternal(): Result = {
		if (feedback.assessmentGroups.isEmpty) {
			CheckSitsUploadCommand.Result(hasAssessmentGroups = false)
		} else {
			exportFeedbackToSitsService.getPartialMatchingSasRecords(feedbackForSits.get) match {
				case Seq() =>
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = false
					)
				case Seq(singleRow) =>
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = true,
						hasWritableMark = singleRow.actualMark.isEmpty || singleRow.uploader == ExportFeedbackToSitsService.tabulaIdentifier,
						hasWritableGrade = singleRow.actualGrade == null || singleRow.uploader == ExportFeedbackToSitsService.tabulaIdentifier
					)
				case rows =>
					val hasWritableMarkAndGrade = rows.exists(row =>
						(row.actualMark.isEmpty || row.uploader == ExportFeedbackToSitsService.tabulaIdentifier)
							&& (row.actualGrade == null || row.uploader == ExportFeedbackToSitsService.tabulaIdentifier)
					)
					CheckSitsUploadCommand.Result(
						hasAssessmentGroups = true,
						hasAssignmentRow = true,
						hasWritableMark = hasWritableMarkAndGrade,
						hasWritableGrade = hasWritableMarkAndGrade
					)
			}
		}
	}

}

trait CheckSitsUploadValidation extends SelfValidating {

	self: CheckSitsUploadCommandState =>

	override def validate(errors: Errors) {
		if (feedbackForSits.isEmpty) {
			errors.reject("feedback.feedbackForSits.missing")
		}
	}

}

trait CheckSitsUploadPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CheckSitsUploadCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		HibernateHelpers.initialiseAndUnproxy(feedback) match {
			case assignmentFeedback: AssignmentFeedback =>
				p.PermissionCheck(Permissions.AssignmentFeedback.Publish, assignmentFeedback)
			case examFeedback: ExamFeedback =>
				p.PermissionCheck(Permissions.ExamFeedback.Manage, examFeedback)
		}
	}

}

trait CheckSitsUploadCommandState {

	self: FeedbackForSitsServiceComponent =>

	def feedback: Feedback
	lazy val feedbackForSits: Option[FeedbackForSits] = feedbackForSitsService.getByFeedback(feedback)
}

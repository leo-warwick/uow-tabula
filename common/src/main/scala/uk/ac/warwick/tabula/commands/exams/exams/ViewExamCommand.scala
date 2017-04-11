package uk.ac.warwick.tabula.commands.exams.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class ViewExamCommandResult(
	students: Seq[User],
	seatNumberMap: Map[User, Option[Int]],
	feedbackMap: Map[User, Option[ExamFeedback]],
	sitsStatusMap: Map[Feedback, Option[FeedbackForSits]]
)

object ViewExamCommand {
	def apply(module: Module, academicYear: AcademicYear, exam: Exam) =
		new ViewExamCommandInternal(module, academicYear, exam)
			with AutowiringFeedbackServiceComponent
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[ViewExamCommandResult]
			with ViewExamPermissions
			with ViewExamCommandState
			with ReadOnly with Unaudited
}


class ViewExamCommandInternal(val module: Module, val academicYear: AcademicYear, val exam: Exam)
	extends CommandInternal[ViewExamCommandResult] {

	self: FeedbackServiceComponent with AssessmentMembershipServiceComponent with FeedbackForSitsServiceComponent =>

	override def applyInternal(): ViewExamCommandResult = {
		val studentSeats = assessmentMembershipService.determineMembershipUsersWithOrder(exam)
		val studentUsers = studentSeats.map(_._1)
		val feedbackMap = feedbackService.getExamFeedbackMap(exam, studentUsers).mapValues(Option(_)).withDefaultValue(None)
		val sitsStatusMap = feedbackForSitsService.getByFeedbacks(feedbackMap.values.flatten.toSeq).mapValues(Option(_)).withDefaultValue(None)
		ViewExamCommandResult(studentUsers, studentSeats.toMap, feedbackMap, sitsStatusMap)
	}

}

trait ViewExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(exam, module)
		p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
	}

}

trait ViewExamCommandState {
	def module: Module
	def academicYear: AcademicYear
	def exam: Exam
}

package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment, Exam, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object FeedbackAdjustmentListCommand {
	def apply(assessment: Assessment) =
		new FeedbackAdjustmentListCommandInternal(assessment)
			with ComposableCommand[Seq[StudentInfo]]
			with FeedbackAdjustmentListCommandPermissions
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with Unaudited
			with ReadOnly
}

case class StudentInfo(student: User, feedback: Option[Feedback])

class FeedbackAdjustmentListCommandInternal(val assessment: Assessment)
	extends CommandInternal[Seq[StudentInfo]] with FeedbackAdjustmentListCommandState {

	self: UserLookupComponent with AssessmentMembershipServiceComponent =>

	def applyInternal(): Seq[StudentInfo] = {
		if (assessment.collectMarks) {
			val allFeedback = assessment.fullFeedback
			val allStudents =
				if (students.isEmpty) assessmentMembershipService.determineMembershipUsers(assessment)
				else students.asScala.map(userLookup.getUserByUserId)

			val (hasFeedback, noFeedback) = allStudents.map { student =>
				StudentInfo(student, allFeedback.find { _.usercode == student.getUserId })
			}.partition { _.feedback.isDefined }

			hasFeedback ++ noFeedback
		} else {
			Seq()
		}

	}
}

trait FeedbackAdjustmentListCommandState {
	def assessment: Assessment
	var students: JList[String] = JArrayList()
}

trait FeedbackAdjustmentListCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackAdjustmentListCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
			case assignment: Assignment =>
				p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
			case exam: Exam =>
				p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
		}
	}
}
package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, MemberOrUser, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AssessmentServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}
import uk.ac.warwick.userlookup.User

object StudentCourseworkUpcomingCommand {
	def apply(memberOrUser: MemberOrUser): Appliable[StudentAssignments] =
		new StudentCourseworkUpcomingCommandInternal(memberOrUser)
			with ComposableCommand[StudentAssignments]
			with StudentCourseworkUpcomingCommandPermissions
			with AutowiringAssessmentServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringFeaturesComponent
			with StudentCourseworkCommandHelper
			with ReadOnly with Unaudited
}

class StudentCourseworkUpcomingCommandInternal(val memberOrUser: MemberOrUser) extends StudentCourseworkCommandInternal
	with StudentCourseworkUpcomingCommandState {

	self: AssessmentServiceComponent with
		  AssessmentMembershipServiceComponent with
		  FeaturesComponent with
			StudentCourseworkCommandHelper =>

	override lazy val overridableAssignmentsWithFeedback = Nil
	override lazy val overridableAssignmentsWithSubmission = Nil

	// find enrolled assignments that require submission in the next month
	override lazy val overridableEnrolledAssignments: Seq[Assignment] = {
		val user = memberOrUser.asUser
		val monthFromNow = DateTime.now.plusMonths(1)

		assessmentMembershipService
			.getEnrolledAssignments(user)
			.filter(a => a.submittable(user) && !a.openEnded && a.submissionDeadline(user).isBefore(monthFromNow) && a.submissionDeadline(user).isAfterNow)
			.sortBy(_.submissionDeadline(user))(Ordering.fromLessThan(_ isBefore _))
	}

	override val usercode: String = memberOrUser.usercode
	override val user: User = memberOrUser.asUser

	override def applyInternal() = StudentAssignments(
		enrolledAssignments = enrolledAssignmentsInfo,
		historicAssignments = Nil
	)

}

trait StudentCourseworkUpcomingCommandState {
	def memberOrUser: MemberOrUser
}

trait StudentCourseworkUpcomingCommandPermissions extends RequiresPermissionsChecking {
	self: StudentCourseworkUpcomingCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		memberOrUser.asMember.foreach { member =>
			p.PermissionCheck(Permissions.Profiles.Read.Coursework, member)
			p.PermissionCheck(Permissions.Submission.Read, member)
			p.PermissionCheck(Permissions.AssignmentFeedback.Read, member)
			p.PermissionCheck(Permissions.Extension.Read, member)
		}
	}
}

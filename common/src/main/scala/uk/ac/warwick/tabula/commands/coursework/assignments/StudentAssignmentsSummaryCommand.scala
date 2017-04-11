package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.StudentAssignmentsSummaryCommand.Result
import uk.ac.warwick.tabula.data.model.EnhancedAssignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AssessmentServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

object StudentAssignmentsSummaryCommand {

	case class Result(
		upcoming: Seq[EnhancedAssignment],
		todo: Seq[EnhancedAssignment],
		doing: Seq[EnhancedAssignment],
		done: Seq[EnhancedAssignment]
	)

	def apply(student: MemberOrUser, academicYearOption: Option[AcademicYear] = None) =
		new StudentAssignmentsSummaryCommandInternal(student, academicYearOption)
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringAssessmentServiceComponent
			with ComposableCommand[StudentAssignmentsSummaryCommand.Result]
			with StudentAssignmentsSummaryPermissions
			with StudentAssignmentsSummaryCommandState
			with ReadOnly with Unaudited
}


class StudentAssignmentsSummaryCommandInternal(val student: MemberOrUser, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[StudentAssignmentsSummaryCommand.Result] with TaskBenchmarking {

	self: AssessmentMembershipServiceComponent with AssessmentServiceComponent =>

	override def applyInternal(): Result = {
		val studentUser = student.asUser

		val enrolledAssignments = assessmentMembershipService.getEnrolledAssignments(studentUser)

		val done = benchmarkTask("getAssignmentsWithFeedback") {
			assessmentService.getAssignmentsWithFeedback(student.usercode, academicYearOption).map(_.enhance(studentUser)).sortBy(enhancedAssignment =>
				if (enhancedAssignment.submission.nonEmpty) {
					enhancedAssignment.submission.get.submittedDate
				} else {
					enhancedAssignment.feedback.flatMap(f => Some(f.releasedDate)).getOrElse(new DateTime())
				}
			)
		}
		val doing = benchmarkTask("getAssignmentsWithSubmission") {
			assessmentService.getAssignmentsWithSubmission(student.usercode, academicYearOption)
				.filterNot(done.map(_.assignment).contains)
				.map(_.enhance(studentUser))
				.sortBy(enhancedAssignment =>
					if (enhancedAssignment.assignment.feedbackDeadline.nonEmpty) {
						enhancedAssignment.assignment.feedbackDeadline.get.toDateTimeAtStartOfDay
					} else {
						enhancedAssignment.submission.map(_.submittedDate).getOrElse(new DateTime())
					}
				)
		}

		val todo = enrolledAssignments
			.filterNot(done.map(_.assignment).contains)
			.filterNot(doing.map(_.assignment).contains)
			.filter(a => academicYearOption.isEmpty || academicYearOption.contains(a.academicYear))
			.filter(a => a.submittable(studentUser))
			.map(_.enhance(studentUser))
			.sortBy(_.submissionDeadline.getOrElse(new DateTime().plusYears(500))) // Sort open-ended assignments to the bottom

		val upcoming = enrolledAssignments
			.filterNot(done.map(_.assignment).contains)
			.filterNot(doing.map(_.assignment).contains)
			.filterNot(todo.map(_.assignment).contains)
			.filter(a => academicYearOption.isEmpty || academicYearOption.contains(a.academicYear))
			.filter(a => a.isVisibleToStudents && a.collectSubmissions)
			.map(_.enhance(studentUser))
			.sortBy(_.submissionDeadline.getOrElse(new DateTime().plusYears(500))) // Sort open-ended assignments to the bottom

		StudentAssignmentsSummaryCommand.Result(upcoming, todo, doing, done)
	}

}

trait StudentAssignmentsSummaryPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentAssignmentsSummaryCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		student.asMember.foreach { member =>
			p.PermissionCheck(Permissions.Profiles.Read.Coursework, member)
			p.PermissionCheck(Permissions.Submission.Read, member)
			p.PermissionCheck(Permissions.AssignmentFeedback.Read, member)
			p.PermissionCheck(Permissions.Extension.Read, member)
		}
	}

}

trait StudentAssignmentsSummaryCommandState {
	def student: MemberOrUser
	def academicYearOption: Option[AcademicYear]
}

package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent, AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Assignment, StudentCourseYearDetails, StudentMember, Submission}
import uk.ac.warwick.tabula.commands.coursework.assignments.{StudentCourseworkCommandHelper, StudentCourseworkFullScreenCommandInternal, StudentCourseworkGadgetCommandInternal}

class OldStudentCourseworkControllerTest extends TestBase with Mockito {

	val student: StudentMember = Fixtures.student()
	val scyd: StudentCourseYearDetails = student.defaultYearDetails.get

	private trait Fixture {
		val assignment: Assignment = newDeepAssignment("LA101")
		assignment.closeDate = new DateTime()

		val submission: Submission = Fixtures.submission("0000001")
		submission.submittedDate = new DateTime()
	}

	trait CommandTestSupport extends AssessmentServiceComponent
			with AssessmentMembershipServiceComponent
			with FeaturesComponent {
		override val assessmentService: AssessmentService = smartMock[AssessmentService]
		override val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
		override val features: FeaturesImpl = {
			val f = Features.empty
			f.assignmentMembership = true
			f
		}

		assessmentService.getAssignmentsWithFeedback(any[StudentCourseYearDetails]) returns Seq()
		assessmentService.filterAssignmentsByCourseAndYear(any[Seq[Assignment]], any[StudentCourseYearDetails]) returns Seq()
		assessmentService.getAssignmentsWithSubmission(any[StudentCourseYearDetails]) returns Seq()
		assessmentMembershipService.getEnrolledAssignments(any[User]) returns Seq()
		assessmentService.getAssignmentsWithFeedback(any[String]) returns Seq()
		assessmentService.getAssignmentsWithSubmission(any[String]) returns Seq()
	}

	@Test
	def historicalAssignmentsNoDupes {
		new Fixture {
			val  assignmentInfo = Map("assignment" -> assignment,
				"submission" -> Option(submission))

			// A late formative assignment that has also been submitted will appear in both of the following
			val assignmentsWithSubmissionInfo = Seq(assignmentInfo)
			val lateFormativeAssignmentsInfo = Seq(assignmentInfo)

			val gadgetCommand = new StudentCourseworkGadgetCommandInternal(scyd) with CommandTestSupport with StudentCourseworkCommandHelper
			val historicalAssignmentsInfo1: Seq[this.gadgetCommand.AssignmentInfo] = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo1.size should be(1)

			val memberOrUser = MemberOrUser(Fixtures.user())
			val fullScreenCommand = new StudentCourseworkFullScreenCommandInternal(memberOrUser) with CommandTestSupport with StudentCourseworkCommandHelper
			val historicalAssignmentsInfo2: Seq[this.gadgetCommand.AssignmentInfo] = gadgetCommand.getHistoricAssignmentsInfo(Nil, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
			historicalAssignmentsInfo2.size should be(1)
		}
	}

}

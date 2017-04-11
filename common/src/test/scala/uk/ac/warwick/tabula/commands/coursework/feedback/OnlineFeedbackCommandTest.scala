package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User


class OnlineFeedbackCommandTest extends TestBase with Mockito {

	trait Fixture {

		// user1 and user3 deliberately have same uniId, to test members with multiple logins.
		val marker: User = Fixtures.user(universityId="marker", userId="marker")

		val user1: User = Fixtures.user(universityId="user1", userId="user1")
		val user2: User = Fixtures.user(universityId="user2", userId="user2")
		val user3: User = Fixtures.user(universityId="user1", userId="user3")

		val assignment = new Assignment
		val module = new Module
		assignment.module = module

		val submission1 = new Submission {
			usercode = "user1"
		}

		val feedback1 = new AssignmentFeedback
		feedback1.assignment = assignment
		val feedback2: AssignmentFeedback = Fixtures.assignmentFeedback("user2", "user2")
		feedback2.released = true
		feedback2.assignment = assignment

		assignment.submissions.add(submission1)
		assignment.feedbacks.add(feedback2)

		val command = new OnlineFeedbackCommand(module, assignment, new CurrentUser(marker, marker)) with OnlineFeedbackCommandTestSupport

		command.assessmentMembershipService.determineMembershipUsers(assignment) returns Seq(user1, user2, user3)

		command.userLookup.registerUserObjects(user1,user2)

		command.submissionService.getSubmissionByUsercode(assignment, "user1") returns Some(submission1)
		command.submissionService.getSubmissionByUsercode(assignment, "user2") returns None
		command.submissionService.getSubmissionByUsercode(assignment, "user3") returns None

		command.feedbackService.getAssignmentFeedbackByUsercode(assignment, "user1") returns Some(feedback1)
		command.feedbackService.getAssignmentFeedbackByUsercode(assignment, "user2") returns Some(feedback2)
		command.feedbackService.getAssignmentFeedbackByUsercode(assignment, "user3") returns None
	}


	@Test
	def commandApply() {
		new Fixture {
			val feedbackGraphs: Seq[StudentFeedbackGraph] = command.applyInternal()
			verify(command.feedbackService, times(1)).getAssignmentFeedbackByUsercode(assignment, "user1")
			verify(command.feedbackService, times(1)).getAssignmentFeedbackByUsercode(assignment, "user2")

			verify(command.submissionService, times(1)).getSubmissionByUsercode(assignment, "user1")
			verify(command.submissionService, times(1)).getSubmissionByUsercode(assignment, "user2")

			val graph1: StudentFeedbackGraph = feedbackGraphs.head
			graph1.student should be(user1)
			graph1.hasSubmission should be {true}
			graph1.hasUncompletedFeedback should be {true}
			graph1.hasPublishedFeedback should be {false}
			graph1.hasCompletedFeedback should be {false}

			val graph2: StudentFeedbackGraph = feedbackGraphs(1)
			graph2.student should be(user2)
			graph2.hasSubmission should be {false}
			graph2.hasUncompletedFeedback should be {true}
			graph2.hasPublishedFeedback should be {true}
			graph2.hasCompletedFeedback should be {false}

			val graph3: StudentFeedbackGraph = feedbackGraphs(2)
			graph3.student should be(user3)
			graph3.hasSubmission should be {false}
			graph3.hasUncompletedFeedback should be {false}
			graph3.hasPublishedFeedback should be {false}
			graph3.hasCompletedFeedback should be {false}

			feedbackGraphs.size should be(3)
		}
	}

}

// Implements the dependencies declared by the command
trait OnlineFeedbackCommandTestSupport extends SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent with AssessmentMembershipServiceComponent with Mockito {
	val userLookup = new MockUserLookup
	val submissionService: SubmissionService = mock[SubmissionService]
	val feedbackService: FeedbackService = mock[FeedbackService]
	var assessmentMembershipService: AssessmentMembershipService = mock[AssessmentMembershipService]
	def apply(): Seq[StudentFeedbackGraph] = Seq()
}
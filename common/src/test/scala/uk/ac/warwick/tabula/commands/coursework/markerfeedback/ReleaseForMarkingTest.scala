package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.coursework.assignments.ReleaseForMarkingCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class ReleaseForMarkingTest extends TestBase with Mockito {

	trait TestSupport extends AssessmentServiceComponent with StateServiceComponent with FeedbackServiceComponent {

		val assessmentService: AssessmentService = smartMock[AssessmentService]
		val stateService: StateService = smartMock[StateService]
		val feedbackService: FeedbackService = smartMock[FeedbackService]
		def apply(): List[Feedback] = List()
	}

	@Test
	def testIsReleased() {
		withUser("cuslaj") {

			val assignment = newDeepAssignment()
			val allStudentsUserGroup = UserGroup.ofUniversityIds
			allStudentsUserGroup.includedUserIds = Seq("0678022","1170836","9170726")
			allStudentsUserGroup.userLookup = new MockUserLookup(true)
			assignment.closeDate = DateTime.parse("2012-08-15T12:00")

			assignment.firstMarkers = Seq(
				FirstMarkersMap(assignment, "marker-uni-id", allStudentsUserGroup)
			).asJava

			generateSubmission(assignment, "0678022")
			generateSubmission(assignment, "1170836")
			generateSubmission(assignment, "9170726")

			trait MockUserLookupComponent extends UserLookupComponent {
				override def userLookup = new MockUserLookup
			}

			// override studentsWithKnownMarkers so we dont have to mock-up a whole workflow
			val command = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with MockUserLookupComponent with TestSupport {
				override def studentsWithKnownMarkers = Seq("0678022", "1170836", "9170726")
			}


			command.students = assignment.submissions.map(_.usercode)
			assignment.feedbacks = command.applyInternal()

			verify(command.stateService, times(3)).updateState(any[MarkerFeedback], any[MarkingState])

			assignment.feedbacks.size should be (3)
			val firstMarkerFeedback = assignment.feedbacks.map(_.firstMarkerFeedback)
			firstMarkerFeedback.size should be (3)
		}
	}


	private def generateSubmission(assignment:Assignment, uniId: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission._universityId = uniId
		submission.usercode = uniId
		assignment.submissions.add(submission)
	}

}

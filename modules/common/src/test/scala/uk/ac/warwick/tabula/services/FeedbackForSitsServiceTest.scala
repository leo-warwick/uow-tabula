package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.{FeedbackForSitsDao, FeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.data.model.FeedbackForSits
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime

class FeedbackForSitsServiceTest extends TestBase with Mockito {

	trait ServiceTestSupport extends FeedbackForSitsDaoComponent {
		val feedbackForSitsDao = smartMock[FeedbackForSitsDao]
	}

	trait Fixture {
		val service = new AbstractFeedbackForSitsService with ServiceTestSupport
		val feedback = Fixtures.feedback("someFeedback")
		val submitter = currentUser
		val gradeGenerator = smartMock[GeneratesGradesFromMarks]
	}

	@Test
	def queueNewFeedbackForSits() = withUser("abcde")	{ new Fixture {
		service.getByFeedback(feedback) returns None
		val feedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

		feedbackForSits.feedback should be(feedback)
		feedbackForSits.initialiser should be(currentUser.apparentUser)
		feedbackForSits.actualGradeLastUploaded should be(null)
		feedbackForSits.actualMarkLastUploaded should be(null)
	}}

	@Test
	def queueExistingFeedbackForSits() = withUser("abcde") { new Fixture {
		val existingFeedbackForSits = new FeedbackForSits
		val grade = "B"
		val mark = 72
		val firstCreatedDate = new DateTime().minusWeeks(2)
		existingFeedbackForSits.actualGradeLastUploaded = grade
		existingFeedbackForSits.actualMarkLastUploaded = mark
		existingFeedbackForSits.initialiser = new User("cuscao")
		existingFeedbackForSits.firstCreatedOn = firstCreatedDate
		existingFeedbackForSits.lastInitialisedOn = firstCreatedDate
		service.getByFeedback(feedback) returns Some(existingFeedbackForSits)

		val feedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

		feedbackForSits.feedback should be(feedback)
		feedbackForSits.initialiser should be(currentUser.apparentUser)
		feedbackForSits.lastInitialisedOn should not be firstCreatedDate
		feedbackForSits.firstCreatedOn should be(firstCreatedDate)
		feedbackForSits.actualGradeLastUploaded should be(grade)
		feedbackForSits.actualMarkLastUploaded should be(mark)
	}}
}

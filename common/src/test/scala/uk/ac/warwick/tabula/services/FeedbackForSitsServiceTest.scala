package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.{FeedbackForSitsDao, FeedbackForSitsDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime

class FeedbackForSitsServiceTest extends TestBase with Mockito {

	trait ServiceTestSupport extends FeedbackForSitsDaoComponent {
		val feedbackForSitsDao: FeedbackForSitsDao = smartMock[FeedbackForSitsDao]
	}

	trait Fixture {
		val service = new AbstractFeedbackForSitsService with ServiceTestSupport
		val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("someFeedback")
		feedback.assignment = new Assignment
		feedback.assignment.module = new Module
		feedback.assignment.module.adminDepartment = new Department
		feedback.assignment.module.adminDepartment.assignmentGradeValidation = true
		feedback.actualMark = Some(100)
		val submitter: CurrentUser = currentUser
		val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
		gradeGenerator.applyForMarks(Map(feedback._universityId -> feedback.actualMark.get)) returns Map(feedback._universityId -> Seq(GradeBoundary(null, "A", 0, 100, "N")))
	}

	@Test
	def queueNewFeedbackForSits() = withUser("abcde")	{ new Fixture {
		service.getByFeedback(feedback) returns None
		val feedbackForSits: FeedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

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
		val firstCreatedDate: DateTime = new DateTime().minusWeeks(2)
		existingFeedbackForSits.actualGradeLastUploaded = grade
		existingFeedbackForSits.actualMarkLastUploaded = mark
		existingFeedbackForSits.initialiser = new User("cuscao")
		existingFeedbackForSits.firstCreatedOn = firstCreatedDate
		existingFeedbackForSits.lastInitialisedOn = firstCreatedDate
		service.getByFeedback(feedback) returns Some(existingFeedbackForSits)

		val feedbackForSits: FeedbackForSits = service.queueFeedback(feedback, submitter, gradeGenerator).get

		feedbackForSits.feedback should be(feedback)
		feedbackForSits.initialiser should be(currentUser.apparentUser)
		feedbackForSits.lastInitialisedOn should not be firstCreatedDate
		feedbackForSits.firstCreatedOn should be(firstCreatedDate)
		feedbackForSits.actualGradeLastUploaded should be(grade)
		feedbackForSits.actualMarkLastUploaded should be(mark)
	}}
}

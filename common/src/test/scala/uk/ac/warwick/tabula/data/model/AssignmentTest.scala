package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.{ExtensionService, FeedbackService, SubmissionService}
import uk.ac.warwick.tabula.{Mockito, Fixtures, TestBase}
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.model.forms.{FormFieldContext, TextField, Extension}

// scalastyle:off magic.number
class AssignmentTest extends TestBase with Mockito {
	@Test def academicYear() {

		withFakeTime(dateTime(2011,NOVEMBER)) {
			new Assignment().academicYear.startYear should be (2011)
		}

		withFakeTime(dateTime(2011,MAY)) {
			new Assignment().academicYear.startYear should be (2010)
		}
	}

	@Test def fields() {
		val assignment = new Assignment
		assignment.findField(Assignment.defaultCommentFieldName) should not be 'defined
		assignment.addDefaultSubmissionFields()
		assignment.findField(Assignment.defaultCommentFieldName) should be ('defined)
		assignment.addDefaultFeedbackFields()
		assignment.findField(Assignment.defaultFeedbackTextFieldName) should be ('defined)

		val ff = new TextField
		ff.name = "pantStyle"
		ff.value = "brief"
		ff.context = FormFieldContext.Feedback
		assignment.addField(ff)
		assignment.findField("pantStyle") should be ('defined)
	}

	@Test(expected=classOf[IllegalArgumentException])
	def fieldContext() {
		val assignment = new Assignment
		val ff = new TextField
		ff.name = "destinedToFail"
		ff.value = "instantly"
		assignment.addField(ff)
	}

	@Test def unreleasedFeedback() {
		val assignment = new Assignment
		assignment.feedbacks should be ('empty)
		assignment.unreleasedFeedback should be ('empty)

		val feedback = mockFeedback(assignment)
		assignment.feedbacks add feedback
		assignment.feedbacks.size should be (1)
		assignment.unreleasedFeedback.size should be (1)
		feedback.released = true
		assignment.unreleasedFeedback should be ('empty)
	}

	@Test def placeholderFeedback() {
		val assignment = new Assignment
		assignment.fullFeedback should be ('empty)
		assignment.markingWorkflow = new FirstMarkerOnlyWorkflow

		val feedback = new AssignmentFeedback
		feedback.assignment = assignment
		assignment.feedbacks add feedback
		assignment.fullFeedback should be ('empty)

		feedback.firstMarkerFeedback = new MarkerFeedback { state = MarkingCompleted }
		feedback.actualMark = Option(41)
		assignment.fullFeedback.size should be (1)
	}

	@Test def submissionsReport() {
		val assignment = new Assignment
		assignment.collectSubmissions = false
		assignment.collectMarks = false
		assignment.submissionsReport should not be 'hasProblems

		for (i <- 1 to 10) {// 0000001 .. 0000010
			val feedback = Fixtures.assignmentFeedback(universityId = idFormat(i))
			feedback.assignment = assignment
			feedback.actualMark = Some(i*10)
			assignment.feedbacks add feedback
		}

		for (i <- 8 to 20) // 0000008 .. 0000020
			assignment.submissions add new Submission {
				usercode = idFormat(i)
				_universityId = idFormat(i)
			}

		// only 0000008 .. 0000010 are common to both lists
		val report = assignment.submissionsReport
		assignment.collectSubmissions = false
		report should not be 'hasProblems // be has problems.
		assignment.collectSubmissions = true
		report should be ('hasProblems) // be has problems.
		report.feedbackOnly.toSeq.sorted should be ((1 to 7) map idFormat)
		report.submissionOnly.toSeq.sorted should be ((11 to 20) map idFormat)
	}

	@Test def openEnded() {
		val assignment = new Assignment
		assignment.openEnded = false

		// past assignment should be closed
		assignment.openDate = new DateTime().minusDays(3)
		assignment.closeDate = new DateTime().minusDays(2)
		assignment.isClosed should be {true}

		// Open Gangnam Style was so 2012
		assignment.openEnded = true
		assignment.isClosed should be {false}
	}

	@Test def assignmentCanPublishFeedback() {
		val assignment = new Assignment
		assignment.feedbacks add mockFeedback(assignment)

		assignment.openDate = new DateTime().minusDays(3)
		assignment.closeDate = new DateTime().plusDays(10)
		assignment.openEnded = false
		// can't publish until closed
		assignment.canPublishFeedback should be {false}

		// unless open-ended
		assignment.openEnded = true
		assignment.canPublishFeedback should be {true}

		// now it's closed
		assignment.openEnded = false
		assignment.closeDate = new DateTime().minusDays(1)
		assignment.canPublishFeedback should be {true}
	}

	@Test def inBetweenDays() {
		val assignment = new Assignment
		assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
		assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 0, 0, 0, 0)
		assignment.openEnded = false

		assignment.isOpened(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be {false}
		assignment.isOpened(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be {true}

		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be {false}
		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be {true}

		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be {false}
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be {true}
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be {false}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) {
			assignment.isOpened should be {false}
			assignment.isClosed should be {false}
			assignment.isBetweenDates() should be {false}
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) {
			assignment.isOpened should be {true}
			assignment.isClosed should be {false}
			assignment.isBetweenDates() should be {true}
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) {
			assignment.isOpened should be {true}
			assignment.isClosed should be {true}
			assignment.isBetweenDates() should be {false}
		}

		assignment.openEnded = true

		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be {false}
		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be {false}

		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be {false}
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be {true}
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be {true}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) {
			assignment.isOpened should be {false}
			assignment.isClosed should be {false}
			assignment.isBetweenDates() should be {false}
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) {
			assignment.isOpened should be {true}
			assignment.isClosed should be {false}
			assignment.isBetweenDates() should be {true}
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) {
			assignment.isOpened should be {true}
			assignment.isClosed should be {false}
			assignment.isBetweenDates() should be {true}
		}
	}

	@Test def assignmentIsLate() {
		val assignment = new Assignment
		assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
		assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 0, 0, 0, 0)
		assignment.openEnded = false

		val submission = new Submission
		submission.usercode = "cuscav"

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)

		assignment.isLate(submission) should be {false}
		assignment.isAuthorisedLate(submission) should be {false}

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)

		assignment.isLate(submission) should be {false}
		assignment.isAuthorisedLate(submission) should be {false}

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)

		assignment.isLate(submission) should be {true}
		assignment.isAuthorisedLate(submission) should be {false}

		val extension = new Extension
		extension.usercode = "cuscav"
		extension.approve()
		extension.expiryDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 12, 0, 0, 0)

		assignment.extensions.add(extension)

		assignment.isLate(submission) should be {false}
		assignment.isAuthorisedLate(submission) should be {true}

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 0, 0, 0, 0)

		assignment.isLate(submission) should be {true}
		assignment.isAuthorisedLate(submission) should be {false}

		assignment.openEnded = true

		assignment.isLate(submission) should be {false}
		assignment.isAuthorisedLate(submission) should be {false}
	}

	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int): String = "%07d" format i

	def mockFeedback(assignment: Assignment): AssignmentFeedback = {
		val f = new AssignmentFeedback()
		f.assignment = assignment
		// add a mark so this is not treated like a placeholder
		f.actualMark = Some(41)
		f
	}

	@Test def workingDaysLate() {
		val assignment = new Assignment
		assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
		assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 12, 0, 0, 0) // Wednesday, 12pm
		assignment.openEnded = false

		val submission = new Submission
		submission.usercode = "cuscav"

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)
		assignment.workingDaysLate(submission) should be (0)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 13, 0, 0, 0) // Wednesday, 1pm
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0) // Thursday (morning), midnight
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 14, 0, 0, 0) // Thursday 2pm
		assignment.workingDaysLate(submission) should be (2)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 11, 0, 0, 0) // Saturday 11am
		assignment.workingDaysLate(submission) should be (3)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 14, 0, 0, 0) // Saturday 2pm
		assignment.workingDaysLate(submission) should be (3)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 14, 0, 0, 0) // Sunday 2pm
		assignment.workingDaysLate(submission) should be (3)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 11, 0, 0, 0) // Monday 11am
		assignment.workingDaysLate(submission) should be (3)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 23, 0, 0, 0) // Monday 11pm
		assignment.workingDaysLate(submission) should be (4)

		// Extended until 12pm Friday
		val extension = new Extension
		extension.usercode = "cuscav"
		extension.approve()
		extension.expiryDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 12, 0, 0, 0)

		assignment.extensions.add(extension)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)
		assignment.workingDaysLate(submission) should be (0)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 13, 0, 0, 0) // Wednesday, 1pm
		assignment.workingDaysLate(submission) should be (0)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0) // Thursday (morning), midnight
		assignment.workingDaysLate(submission) should be (0)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 14, 0, 0, 0) // Thursday 2pm
		assignment.workingDaysLate(submission) should be (0)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 11, 0, 0, 0) // Saturday 11am
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 14, 0, 0, 0) // Saturday 2pm
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 14, 0, 0, 0) // Sunday 2pm
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 11, 0, 0, 0) // Monday 11am
		assignment.workingDaysLate(submission) should be (1)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 23, 0, 0, 0) // Monday 11pm
		assignment.workingDaysLate(submission) should be (2)
	}

	@Test def testHasOutstandingFeedback(): Unit = {
		val assignment = new Assignment
		assignment.dissertation = false
		assignment.openDate = new DateTime(2015, DateTimeConstants.APRIL, 1, 12, 0, 0, 0)
		assignment.closeDate = assignment.openDate.plusWeeks(2)

		assignment.submissionService = smartMock[SubmissionService]
		assignment.feedbackService = smartMock[FeedbackService]
		assignment.extensionService = smartMock[ExtensionService]

		assignment.extensionService.hasExtensions(assignment) returns false

		assignment.submissionService.getSubmissionsByAssignment(assignment) returns Nil

		assignment.hasOutstandingFeedback should be (false)

		reset(assignment.submissionService)

		val submission1 = new Submission
		submission1.usercode = "0000001"
		submission1.assignment = assignment

		val submission2 = new Submission
		submission2.usercode = "0000002"
		submission2.assignment = assignment

		assignment.submissionService.getSubmissionsByAssignment(assignment) returns Seq(submission1, submission2)
		assignment.openEnded = true // so submissions don't need a feedback deadline

		assignment.hasOutstandingFeedback should be (false)

		assignment.openEnded = false

		assignment.feedbackService.getAssignmentFeedbackByUsercode(assignment, "0000001") returns None
		assignment.feedbackService.getAssignmentFeedbackByUsercode(assignment, "0000002") returns None

		assignment.hasOutstandingFeedback should be (true)

		reset(assignment.feedbackService)

		// Publish feedback for student 1, add feedback for student 2 but don't publish it
		val feedback1 = new AssignmentFeedback
		feedback1.released = true

		val feedback2 = new AssignmentFeedback
		feedback2.released = false

		assignment.feedbackService.getAssignmentFeedbackByUsercode(assignment, "0000001") returns Some(feedback1)
		assignment.feedbackService.getAssignmentFeedbackByUsercode(assignment, "0000002") returns Some(feedback2)

		assignment.hasOutstandingFeedback should be (true)

		// Once feedback2 is released, there's no longer outstanding feedback
		feedback2.released = true

		assignment.hasOutstandingFeedback should be (false)
	}

}
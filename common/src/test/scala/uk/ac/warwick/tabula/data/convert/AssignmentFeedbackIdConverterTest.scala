package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Feedback}

class AssignmentFeedbackIdConverterTest extends TestBase with Mockito {

	val converter = new AssignmentFeedbackIdConverter
	var service: FeedbackDao = mock[FeedbackDao]
	converter.service = service

	@Test def validInput {
		val feedback = new AssignmentFeedback
		feedback.id = "steve"

		service.getAssignmentFeedback("steve") returns (Some(feedback))

		converter.convertRight("steve") should be (feedback)
	}

	@Test def invalidInput {
		service.getAssignmentFeedback("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val feedback = new AssignmentFeedback
		feedback.id = "steve"

		converter.convertLeft(feedback) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
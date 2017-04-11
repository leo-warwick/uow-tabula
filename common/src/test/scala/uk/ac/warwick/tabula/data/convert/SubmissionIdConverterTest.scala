package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.SubmissionService

class SubmissionIdConverterTest extends TestBase with Mockito {

	val converter = new SubmissionIdConverter
	val service: SubmissionService = mock[SubmissionService]
	converter.service = service

	@Test def validInput {
		val submission = new Submission
		submission.id = "steve"

		service.getSubmission("steve") returns (Some(submission))

		converter.convertRight("steve") should be (submission)
	}

	@Test def invalidInput {
		service.getSubmission("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val submission = new Submission
		submission.id = "steve"

		converter.convertLeft(submission) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
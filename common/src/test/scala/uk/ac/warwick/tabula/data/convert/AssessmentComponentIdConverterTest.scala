package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.AssessmentComponent
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.Mockito

class AssessmentComponentIdConverterTest extends TestBase with Mockito {

	val converter = new AssessmentComponentIdConverter
	val service: AssessmentMembershipService = mock[AssessmentMembershipService]
	converter.service = service

	@Test def validInput {
		val assignment = new AssessmentComponent
		assignment.id = "steve"

		service.getAssessmentComponent("steve") returns (Some(assignment))

		converter.convertRight("steve") should be (assignment)
	}

	@Test def invalidInput {
		service.getAssessmentComponent("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val assignment = new AssessmentComponent
		assignment.id = "steve"

		converter.convertLeft(assignment) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
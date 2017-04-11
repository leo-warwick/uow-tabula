package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.AssessmentGroup
import uk.ac.warwick.tabula.services.AssessmentMembershipService

class AssessmentGroupIdConverterTest extends TestBase with Mockito {

	val converter = new AssessmentGroupIdConverter
	val service: AssessmentMembershipService = mock[AssessmentMembershipService]
	converter.service = service

	@Test def validInput {
		val group = new AssessmentGroup
		group.id = "steve"

		service.getAssessmentGroup("steve") returns (Some(group))

		converter.convertRight("steve") should be (group)
	}

	@Test def invalidInput {
		service.getAssessmentGroup("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val group = new AssessmentGroup
		group.id = "steve"

		converter.convertLeft(group) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
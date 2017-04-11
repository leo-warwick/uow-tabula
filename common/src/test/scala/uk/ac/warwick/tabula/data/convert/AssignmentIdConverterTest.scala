package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssessmentService

class AssignmentIdConverterTest extends TestBase with Mockito {

	val converter = new AssignmentIdConverter
	val service: AssessmentService = mock[AssessmentService]
	converter.service = service

	@Test def validInput {
		val assignment = new Assignment
		assignment.id = "steve"

		service.getAssignmentById("steve") returns (Some(assignment))

		converter.convertRight("steve") should be (assignment)
	}

	@Test def invalidInput {
		service.getAssignmentById("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val assignment = new Assignment
		assignment.id = "steve"

		converter.convertLeft(assignment) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
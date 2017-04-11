package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class AssessmentComponentIdConverter extends TwoWayConverter[String, AssessmentComponent] {

	@Autowired var service: AssessmentMembershipService = _

	// Converter used for binding request
	override def convertRight(id: String): AssessmentComponent = service.getAssessmentComponent(id).orNull

	// Formatter used for generating textual value in template
	override def convertLeft(assignment: AssessmentComponent): String = (Option(assignment) map { _.id }).orNull

}
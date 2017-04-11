package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class AssignmentIdConverter extends TwoWayConverter[String, Assignment] {

	@Autowired var service: AssessmentService = _

	override def convertRight(id: String): Assignment = (Option(id) flatMap { service.getAssignmentById }).orNull
	override def convertLeft(assignment: Assignment): String = (Option(assignment) map {_.id}).orNull

}
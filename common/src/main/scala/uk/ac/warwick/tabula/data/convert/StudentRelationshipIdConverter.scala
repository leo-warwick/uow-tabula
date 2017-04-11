package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class StudentRelationshipIdConverter extends TwoWayConverter[String, StudentRelationship] {

	@Autowired var service: RelationshipService = _

	override def convertRight(id: String): StudentRelationship = (Option(id) flatMap { service.getStudentRelationshipById }).orNull
	override def convertLeft(relationshipType: StudentRelationship): String = (Option(relationshipType) map {_.id}).orNull

}
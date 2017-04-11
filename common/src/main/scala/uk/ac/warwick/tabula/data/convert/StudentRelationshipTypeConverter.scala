package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class StudentRelationshipTypeConverter extends TwoWayConverter[String, StudentRelationshipType] {

	@Autowired var service: RelationshipService = _

	override def convertRight(urlPart: String): StudentRelationshipType = (Option(urlPart) flatMap { service.getStudentRelationshipTypeByUrlPart }).orNull
	override def convertLeft(relationshipType: StudentRelationshipType): String = (Option(relationshipType) map {_.urlPart}).orNull

}
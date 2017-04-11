package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.AssessmentGroup
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.AssessmentMembershipService

class AssessmentGroupIdConverter extends TwoWayConverter[String, AssessmentGroup] {

	@Autowired var service: AssessmentMembershipService = _

	// Converter used for binding request
	override def convertRight(id: String): AssessmentGroup = service.getAssessmentGroup(id).orNull

	// Formatter used for generating textual value in template
	override def convertLeft(group: AssessmentGroup): String = if (group == null) null else group.id

}

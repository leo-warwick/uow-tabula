package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.ExtensionService

class ExtensionIdConverter extends TwoWayConverter[String, Extension] {

	@Autowired var service: ExtensionService = _

	override def convertRight(id: String): Extension = (Option(id) flatMap { service.getExtensionById(_) }).orNull
	override def convertLeft(extension: Extension): String = (Option(extension) map {_.id}).orNull

}
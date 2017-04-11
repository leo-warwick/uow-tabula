package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class ModuleCodeConverter extends TwoWayConverter[String, Module] {

	@Autowired var service: ModuleAndDepartmentService = _

	override def convertRight(code: String): Module =
		service.getModuleByCode(sanitise(code)).getOrElse {
			service.getModuleById(code).orNull
		}

	override def convertLeft(module: Module): String = (Option(module) map { _.code }).orNull

	def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
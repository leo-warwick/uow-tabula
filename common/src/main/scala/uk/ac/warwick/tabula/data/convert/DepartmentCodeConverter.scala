package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

/**
 * Converts to and from a Department and its lowercase department code.
 */
class DepartmentCodeConverter extends TwoWayConverter[String, Department] {

	@Autowired var service: ModuleAndDepartmentService = _

	override def convertRight(code: String): Department = {
		service.getDepartmentByCode(sanitise(code)).getOrElse {
			service.getDepartmentById(code).orNull
		}
	}

	override def convertLeft(department: Department): String = (Option(department) map { _.code }).orNull

	def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
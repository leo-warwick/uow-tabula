package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Module

class ModuleCodeConverterTest extends TestBase with Mockito {

	val converter = new ModuleCodeConverter
	val service: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	converter.service = service

	@Test def validInput {
		val module = new Module
		module.id = "steve"
		module.code = "in"

		service.getModuleByCode("steve") returns (None)
		service.getModuleByCode("in") returns (Some(module))
		service.getModuleById("steve") returns (Some(module))

		converter.convertRight("in") should be (module)
		converter.convertRight("steve") should be (module)
	}

	@Test def invalidInput {
		service.getModuleByCode("20x6") returns (None)
		service.getModuleById("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val module = new Module
		module.id = "steve"
		module.code = "in"

		converter.convertLeft(module) should be ("in")
		converter.convertLeft(null) should be (null)
	}

}
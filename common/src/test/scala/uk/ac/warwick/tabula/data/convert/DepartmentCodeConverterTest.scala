package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model.Department

class DepartmentCodeConverterTest extends TestBase with Mockito {

	val converter = new DepartmentCodeConverter
	val service: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
	converter.service = service

	@Test def validInput {
		val department = new Department
		department.id = "steve"
		department.code = "in"

		service.getDepartmentByCode("steve") returns (None)
		service.getDepartmentByCode("in") returns (Some(department))
		service.getDepartmentById("steve") returns (Some(department))

		converter.convertRight("in") should be (department)
		converter.convertRight("steve") should be (department)
	}

	@Test def invalidInput {
		service.getDepartmentByCode("20x6") returns (None)
		service.getDepartmentById("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val department = new Department
		department.id = "steve"
		department.code = "in"

		converter.convertLeft(department) should be ("in")
		converter.convertLeft(null) should be (null)
	}

}
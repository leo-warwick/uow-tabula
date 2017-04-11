package uk.ac.warwick.tabula.commands.scheduling.imports

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.{ModuleImporter, ModuleImporterComponent, ModuleInfo}

import scala.reflect._

class ImportModulesCommandTest extends TestBase with MockitoSugar {

	trait Environment {
		// Test data
		val department = new Department {
			code = "cs"
			fullName = "Computer Science"
		}
		val moduleInfos = Seq(
            ModuleInfo("Science for Computers", "CS101", null, DegreeType.Undergraduate),
            ModuleInfo("Computers for Science", "CS102", null, DegreeType.Undergraduate))

        // Mocks
		val mockModuleService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]

		// The class under test, with mocks wired
		val command = new ImportModules with Logging with ModuleImporterComponent with ModuleAndDepartmentServiceComponent {
			val moduleImporter: ModuleImporter = mock[ModuleImporter]
			val moduleAndDepartmentService: ModuleAndDepartmentService = mockModuleService
		}
	}

	@Test
	def createModule {
		new Environment {
			when(mockModuleService.getModuleByCode("CS101")) thenReturn None
			when(mockModuleService.getModuleByCode("CS102")) thenReturn None

			command.importModules(moduleInfos, department)

			verify(mockModuleService, times(2)).saveOrUpdate(isA[Module])
		}
	}

	// HFC-354
	@Test
	def updateModuleName {
		new Environment {
			val existingModule = new Module {
				code = "CS101"
				name = "Brian's fun bus"
			}
			when(mockModuleService.getModuleByCode("CS101")) thenReturn Some(existingModule)
			when(mockModuleService.getModuleByCode("CS102")) thenReturn None

			command.importModules(moduleInfos, department)

			verify(mockModuleService, times(2)).saveOrUpdate(isA[Module])
			existingModule.name should be ("Science for Computers")
		}
	}

	private def isA[A : ClassTag] = {
		org.mockito.Matchers.isA[A](classTag[A].runtimeClass.asInstanceOf[Class[A]])
	}

}
package uk.ac.warwick.tabula.commands.scheduling.imports

import org.hibernate.Session
import org.mockito.Matchers
import uk.ac.warwick.tabula.services.exams.grids.UpstreamModuleListService
import uk.ac.warwick.tabula.services.scheduling.ModuleListImporter
import uk.ac.warwick.tabula.{Mockito, TestBase}

class ImportModuleListsCommandTest extends TestBase with Mockito {

	trait Fixture {
		val mockSession: Session = smartMock[Session]
		val command = new ImportModuleListsCommand {
			override val session: Session = mockSession
		}
		command.upstreamModuleListService = smartMock[UpstreamModuleListService]
		command.moduleListImporter = smartMock[ModuleListImporter]
	}

	@Test
	def doEntries(): Unit = {
		// Check query grouping
		new Fixture {
			command.upstreamModuleListService.countAllModuleLists returns 1248
			command.upstreamModuleListService.listModuleLists(any[Int], Matchers.eq(command.ImportGroupSize)) returns Seq()
			command.moduleListImporter.getModuleListEntries(Seq()) returns Seq()
			command.doEntries()
			verify(command.upstreamModuleListService, times(13)).listModuleLists(any[Int], Matchers.eq(command.ImportGroupSize))
			verify(command.upstreamModuleListService, times(1)).listModuleLists(1, 100)
			verify(command.upstreamModuleListService, times(1)).listModuleLists(1201, 100)
			verify(command.upstreamModuleListService, times(0)).listModuleLists(1301, 100)
		}
	}


}

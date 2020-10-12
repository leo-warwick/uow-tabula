package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.DisabilityInfo
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito}


class ImportDisabilitiesCommandTest extends AppContextTestBase with Mockito with Logging {

  @Transactional
  @Test def testImportDisabilitiesCommand(): Unit = {
    val info = new DisabilityInfo("72", "HERON", "You spear fish like a demon, but struggle in academic environments")

    // test command
    val command = new ImportDisabilitiesCommand(info)
    val (disability, result) = command.applyInternal()
    disability.code should be("72")
    disability.shortName should be("HERON")
    disability.sitsDefinition should be("You spear fish like a demon, but struggle in academic environments")
    disability.lastUpdatedDate.dayOfMonth should be((new DateTime).dayOfMonth)
    disability.tabulaDefinition should be(null)
    result should be(ImportAcademicInformationCommand.ImportResult(added = 1))
  }
}

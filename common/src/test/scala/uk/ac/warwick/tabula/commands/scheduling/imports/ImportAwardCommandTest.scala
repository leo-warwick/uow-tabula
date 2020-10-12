package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.AwardInfo
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito}


class ImportAwardCommandTest extends AppContextTestBase with Mockito with Logging {

  @Transactional
  @Test def testImportAwardCommand(): Unit = {
    val info = new AwardInfo("BSC", "BSc", "Bachelor of Science")

    // test command
    val command = new ImportAwardCommand(info)
    val (award, result) = command.applyInternal()
    award.code should be("BSC")
    award.shortName should be("BSc")
    award.name should be("Bachelor of Science")
    award.lastUpdatedDate.dayOfMonth should be((new DateTime).dayOfMonth)
    result should be(ImportAcademicInformationCommand.ImportResult(added = 1))

  }

}

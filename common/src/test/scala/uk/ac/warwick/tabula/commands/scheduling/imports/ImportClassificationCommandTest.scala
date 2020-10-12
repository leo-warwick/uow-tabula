package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ClassificationInfo
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito}


class ImportClassificationCommandTest extends AppContextTestBase with Mockito with Logging {

  @Transactional
  @Test def testImportClassificationCommand(): Unit = {
    val info = new ClassificationInfo("01", "1 ST CLASS HONS", "First Class")

    // test command
    val command = new ImportClassificationCommand(info)
    val (classification, result) = command.applyInternal()
    classification.code should be("01")
    classification.shortName should be("1 ST CLASS HONS")
    classification.name should be("First Class")
    classification.lastUpdatedDate.dayOfMonth should be((new DateTime).dayOfMonth)
    result should be(ImportAcademicInformationCommand.ImportResult(added = 1))

  }

}

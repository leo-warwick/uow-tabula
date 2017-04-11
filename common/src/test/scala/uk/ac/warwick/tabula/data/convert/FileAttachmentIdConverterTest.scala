package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.Mockito

class FileAttachmentIdConverterTest extends TestBase with Mockito {

	val converter = new FileAttachmentIdConverter
	var service: FileDao = mock[FileDao]
	converter.fileDao = service

	@Test def validInput {
		val file = new FileAttachment
		file.id = "steve"

		service.getFileById("steve") returns (Some(file))

		converter.convertRight("steve") should be (file)
	}

	@Test def invalidInput {
		service.getFileById("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val file = new FileAttachment
		file.id = "steve"

		converter.convertLeft(file) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.ExtensionService
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.Mockito

class ExtensionIdConverterTest extends TestBase with Mockito {

	val converter = new ExtensionIdConverter
	val service: ExtensionService = mock[ExtensionService]
	converter.service = service

	@Test def validInput {
		val extension = new Extension
		extension.id = "steve"

		service.getExtensionById("steve") returns (Some(extension))

		converter.convertRight("steve") should be (extension)
	}

	@Test def invalidInput {
		service.getExtensionById("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val extension = new Extension
		extension.id = "steve"

		converter.convertLeft(extension) should be ("steve")
		converter.convertLeft(null) should be (null)
	}

}
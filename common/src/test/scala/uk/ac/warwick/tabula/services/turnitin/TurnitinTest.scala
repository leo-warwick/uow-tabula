package uk.ac.warwick.tabula.services.turnitin

import uk.ac.warwick.tabula.TestBase


class TurnitinTest extends TestBase {

	@Test
	def documentTitle(): Unit = {
		DocumentTitle("12345", "ext") match {
			case DocumentTitle(id, ext) =>
				id should be("12345")
				ext should be("ext")
		}
		AnyDocumentTitle("12345.ext") match {
			case DocumentTitle(id, ext) =>
				id should be("12345")
				ext should be("ext")
		}
	}

}
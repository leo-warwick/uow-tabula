package uk.ac.warwick.tabula.helpers

import org.springframework.core.io.ClassPathResource
import uk.ac.warwick.tabula.TestBase

class DetectMimeTypeTest extends TestBase {

	@Test
	def detect() {
		Closeables.closeThis(new ClassPathResource("/feedback1.zip").getInputStream) { is =>
			val mimeType = DetectMimeType.detectMimeType(is)
			mimeType should be("application/zip")
		}
	}

}

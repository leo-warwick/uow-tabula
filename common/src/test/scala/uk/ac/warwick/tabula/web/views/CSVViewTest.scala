package uk.ac.warwick.tabula.web.views

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.junit.Test
import uk.ac.warwick.tabula.TestBase

class CSVViewTest extends TestBase {

	@Test
	def encoding {
		val text = "Nick\u2019s Unicode Text"
		val view = new CSVView(csv = text)
		val request = new MockHttpServletRequest
		val response = new MockHttpServletResponse
		view.render(null, request, response)

		response.getContentAsString.trim should be (text)
	}

}
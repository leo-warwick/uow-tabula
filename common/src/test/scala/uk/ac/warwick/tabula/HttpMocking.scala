package uk.ac.warwick.tabula
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

trait HttpMocking {
	def mockRequest = new MockHttpServletRequest
	def mockResponse = new MockHttpServletResponse
}
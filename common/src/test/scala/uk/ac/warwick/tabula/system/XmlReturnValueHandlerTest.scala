package uk.ac.warwick.tabula.system

import org.springframework.core.MethodParameter
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.web.views.XmlView

import scala.xml.Elem

class XmlReturnValueHandlerTest extends TestBase {

	val handler = new XmlReturnValueHandler

	def returnsXml: Elem = {
		<xml>
			<scala>is the best</scala>
		</xml>
	}
	def returnsUnit() {}
	def returnsAny: Any = null

	val xmlMethod  = new MethodParameter(getClass.getMethod("returnsXml"), 1)
	val unitMethod = new MethodParameter(getClass.getMethod("returnsUnit"), 1)
	val anyMethod  = new MethodParameter(getClass.getMethod("returnsAny"), 1)

	@Test def supports() {
		handler.supportsReturnType(xmlMethod) should be (true)
		handler.supportsReturnType(unitMethod) should be (false)
		handler.supportsReturnType(anyMethod) should be (false)
	}

	@Test def handle() {
		val mavContainer = new ModelAndViewContainer
		val req = new ServletWebRequest(new MockHttpServletRequest)
		handler.handleReturnValue(returnsXml, xmlMethod, mavContainer, req)

		mavContainer.getViewName should be (null)
		mavContainer.isRequestHandled should be (false)

		val hsReq = new MockHttpServletRequest
		val hsResp = new MockHttpServletResponse

		mavContainer.getView.asInstanceOf[XmlView].render(null, hsReq, hsResp)

		hsResp.getContentAsString.replaceAll("\\s+", "") should be ("""
				<?xml version="1.0" encoding="UTF-8" ?>
				<xml>
				  <scala>is the best</scala>
				</xml>
				""".replaceAll("\\s+", ""))
	}

}
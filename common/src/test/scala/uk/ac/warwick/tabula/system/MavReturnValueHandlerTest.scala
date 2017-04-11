package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import org.springframework.core.MethodParameter
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class MavReturnValueHandlerTest extends TestBase {

	val handler = new MavReturnValueHandler

	def returnsMav = Mav("mymav")
	def returnsUnit {}
	def returnsAny: Any = null

	class SuperMav(viewName: String) extends Mav(viewName)

	def returnsSuperMav = new SuperMav("supermav")

	val mavMethod  = new MethodParameter(getClass.getMethod("returnsMav"), 1)
	val unitMethod = new MethodParameter(getClass.getMethod("returnsUnit"), 1)
	val anyMethod  = new MethodParameter(getClass.getMethod("returnsAny"), 1)
	val superMavMethod  = new MethodParameter(getClass.getMethod("returnsMav"), 1)

	@Test def supports {
		handler.supportsReturnType(mavMethod) should be (true)
		handler.supportsReturnType(unitMethod) should be (false)
		handler.supportsReturnType(anyMethod) should be (false)
		handler.supportsReturnType(superMavMethod) should be (true)
	}

	@Test def handle {
		val mavContainer = new ModelAndViewContainer
		val req = new ServletWebRequest(new MockHttpServletRequest)
		handler.handleReturnValue(returnsMav, mavMethod, mavContainer, req)

		mavContainer.getViewName should be ("mymav")
		mavContainer.isRequestHandled() should be (false)
	}

	@Test def handleSubtype {
		val mavContainer = new ModelAndViewContainer
		val req = new ServletWebRequest(new MockHttpServletRequest)
		handler.handleReturnValue(returnsSuperMav, superMavMethod, mavContainer, req)

		mavContainer.getViewName should be ("supermav")
		mavContainer.isRequestHandled() should be (false)
	}

}
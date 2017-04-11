package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class ConditionalInterceptorTest extends TestBase with Mockito {

	val delegate: WebRequestInterceptor = mock[WebRequestInterceptor]
	val interceptor = new ConditionalInterceptor(delegate)
	interceptor.excludePath = "/sysadmin/*"
	interceptor.afterPropertiesSet()

	val request = new MockHttpServletRequest
	val servletRequest = new ServletWebRequest(request)

	@Test def nonMatching {
		interceptor.preHandle(servletRequest)
		verify(delegate, times(1)).preHandle(servletRequest)

		interceptor.postHandle(servletRequest, null)
		verify(delegate, times(1)).postHandle(servletRequest, null)

		interceptor.afterCompletion(servletRequest, null)
		verify(delegate, times(1)).afterCompletion(servletRequest, null)
	}

	@Test def matching {
		request.setContextPath("/tabula")
		request.setRequestURI("/tabula/sysadmin/home")

		interceptor.preHandle(servletRequest)
		verify(delegate, times(0)).preHandle(servletRequest)

		interceptor.postHandle(servletRequest, null)
		verify(delegate, times(0)).postHandle(servletRequest, null)

		interceptor.afterCompletion(servletRequest, null)
		verify(delegate, times(0)).afterCompletion(servletRequest, null)
	}

}
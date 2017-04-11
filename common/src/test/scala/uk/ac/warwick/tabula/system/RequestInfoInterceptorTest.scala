package uk.ac.warwick.tabula.system

import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import uk.ac.warwick.tabula.helpers.HttpServletRequestUtils
import uk.ac.warwick.tabula.services.{EmergencyMessageService, MaintenanceModeService}
import uk.ac.warwick.tabula.{CurrentUser, Mockito, RequestInfo, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.Uri

// scalastyle:off magic.number
class RequestInfoInterceptorTest extends TestBase with Mockito {

	val interceptor = new RequestInfoInterceptor

	val maintenanceModeService: MaintenanceModeService = mock[MaintenanceModeService]
	val emergencyMessageService: EmergencyMessageService = mock[EmergencyMessageService]
	interceptor.maintenance = maintenanceModeService
	interceptor.emergencyMessage = emergencyMessageService

	@Test def fromExisting {
		val req = new MockHttpServletRequest
		withUser("cuscav") {
			req.setAttribute(RequestInfoInterceptor.RequestInfoAttribute, RequestInfo.fromThread.get)
		}

		RequestInfo.fromThread should be ('empty)

		val resp = new MockHttpServletResponse

		interceptor.preHandle(req, resp, null) should be (true)

		RequestInfo.fromThread should be ('defined)

		interceptor.afterCompletion(req, resp, null, null)

		RequestInfo.fromThread should be ('empty)
	}

	@Test def withNewEmpty {
		val req = new MockHttpServletRequest
		req.setServerName("tabula.warwick.ac.uk")
		req.setScheme("https")
		req.setServerPort(443)
		req.setRequestURI("/yes/its/me")

		val resp = new MockHttpServletResponse

		interceptor.preHandle(req, resp, null) should be (true)

		RequestInfo.fromThread should be ('defined)
		val info = RequestInfo.fromThread.get

		info.ajax should be (false)
		info.maintenance should be (false)
		info.requestedUri should be (Uri.parse("https://tabula.warwick.ac.uk/yes/its/me"))
		info.requestParameters should be (Map())
		info.user should be (null)

		interceptor.afterCompletion(req, resp, null, null)

		RequestInfo.fromThread should be ('empty)
	}

	@Test def withNew {
		val user = new CurrentUser(new User(), new User())

		val req = new MockHttpServletRequest
		req.setAttribute(CurrentUser.keyName, user)
		req.setParameter("one", Array("two", "three"))
		req.setParameter("yes", Array[String]())
		req.setParameter("i", "love")
		req.addHeader(HttpServletRequestUtils.AjaxHeader, "XMLHttpRequest")
		req.addHeader(HttpServletRequestUtils.XRequestedUriHeader, "https://tabula.warwick.ac.uk/yes/its/me?i=love")

		val resp = new MockHttpServletResponse

		maintenanceModeService.enabled returns (true)

		interceptor.preHandle(req, resp, null) should be (true)

		RequestInfo.fromThread should be ('defined)
		val info = RequestInfo.fromThread.get

		info.ajax should be (true)
		info.maintenance should be (true)
		info.requestedUri should be (Uri.parse("https://tabula.warwick.ac.uk/yes/its/me?i=love"))
		info.requestParameters should be (Map(
				"one" -> List("two", "three"),
				"yes" -> List(),
				"i"   -> List("love")
		))
		info.user should be (user)

		interceptor.afterCompletion(req, resp, null, null)

		RequestInfo.fromThread should be ('empty)
	}

}
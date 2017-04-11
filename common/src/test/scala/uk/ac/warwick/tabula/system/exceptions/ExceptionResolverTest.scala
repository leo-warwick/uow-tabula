package uk.ac.warwick.tabula.system.exceptions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.system.CurrentUserInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.system.RequestInfoInterceptor
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.multipart.MultipartException

// scalastyle:off magic.number
class ExceptionResolverTest extends TestBase {

	val resolver = new ExceptionResolver {
		defaultView = "defaultErrorView"
		exceptionHandler = new ExceptionHandler {
			override def exception(context: ExceptionContext) {
				// no special handling.
			}
		}

		viewMappings = Map(
			classOf[ItemNotFoundException].getName -> "could-not-find",
			classOf[FileUploadException].getName -> "upload-failed"
		)

		override def loginUrl = "https://websignon.example.com/?target=whatever&etc"
	}

	/**
	 * When a PermissionDeniedException is thrown and the user is not
	 * signed in, we should always redirect to sign in rather than present
	 * a permission denied page.
	 */
	@Test
	def permDeniedNotLoggedIn() {
		withUser(null) {
			val requestInfo = RequestInfo.fromThread.get
			val user = requestInfo.user
			val exception = new PermissionDeniedException(user, null, null, null)
			val modelAndView = resolver.doResolve(exception, None)
			modelAndView.viewName should be ("redirect:"+resolver.loginUrl)
		}
	}

	/**
	 * A PermissionDeniedException when logged in should be handled normally.
	 * Usually we'd have an explicit mapping from PermissionDeniedException to
	 * a special view but in this test it'll fall back to the default error view.
	 */
	@Test
	def permDeniedLoggedIn() {
		withUser("cusebr") {
			val requestInfo = RequestInfo.fromThread.get
			val user = requestInfo.user
			val exception = new PermissionDeniedException(user, null, null, null)
			val modelAndView = resolver.doResolve(exception, None)
			modelAndView.viewName should be (resolver.defaultView)
		}
	}

	/**
	 * TAB-567 - When handling multipart exceptions, wrap in a "HandledException" wrapper
	 */
	@Test def multipartException() = withUser("cuscav") {
		val requestInfo = RequestInfo.fromThread.get
		val exception = new MultipartException("Could not parse multipart servlet request")
		val modelAndView = resolver.doResolve(exception, None)
		modelAndView.viewName should be ("upload-failed")
		modelAndView.toModel("exception") match {
			case ex: FileUploadException if ex.getCause == exception => ex.isInstanceOf[HandledException] should be {true}
			case _ => fail("expected file upload exception")
		}
	}

	/**
	 * When an exception mapping exists for an exception, use the
	 * view name it maps to rather than defaultView.
	 */
	@Test
	def viewMapping() {
		withUser("cusebr") {
			val modelAndView = resolver.doResolve(new ItemNotFoundException(), None)
			modelAndView.viewName should be ("could-not-find")
		}
	}

	@Test def callInterceptors() {
		var handled = 0

		resolver.userInterceptor = new CurrentUserInterceptor {
			override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any): Boolean = {
				handled += 7
				true
			}
		}

		resolver.infoInterceptor = new RequestInfoInterceptor {
			override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any): Boolean = {
				handled += 13
				true
			}
		}

		withUser("cusebr") {
			val request = new MockHttpServletRequest
			val response = new MockHttpServletResponse

			val requestInfo = RequestInfo.fromThread.get
			val user = requestInfo.user
			val exception = new PermissionDeniedException(user, null, null, null)
			val modelAndView = resolver.resolveException(request, response, handled, exception)
			modelAndView.getViewName should be (resolver.defaultView)

			handled should be (20)
		}
	}

}
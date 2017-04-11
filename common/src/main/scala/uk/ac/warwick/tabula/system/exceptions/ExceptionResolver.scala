package uk.ac.warwick.tabula.system.exceptions

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.beans.TypeMismatchException
import org.springframework.beans.factory.annotation.{Autowired, Required}
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.multipart.MultipartException
import org.springframework.web.servlet.{HandlerExceptionResolver, ModelAndView}
import org.springframework.web.servlet.view.RedirectView
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.profiles.DefaultPhoto
import uk.ac.warwick.tabula.helpers.HttpServletRequestUtils._
import uk.ac.warwick.tabula.helpers.{Logging, Ordered}
import uk.ac.warwick.tabula.system.{RenderableFileView, CurrentUserInterceptor, RequestInfoInterceptor}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.ControllerViews
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.util.core.ExceptionUtils

import scala.collection.JavaConverters._

/**
 * Implements the Spring HandlerExceptionResolver SPI to catch all errors.
 *
 * Errors not caught by Spring will be forwarded by the web.xml error handler to
 * ErrorController which delegates to ExceptionResolver.doResolve(e), so all errors
 * should come here eventually.
 */
class ExceptionResolver extends HandlerExceptionResolver with Logging with Ordered with ControllerViews {

	@Required var defaultView: String = _

	@Autowired var exceptionHandler: ExceptionHandler = _

	@Autowired var userInterceptor: CurrentUserInterceptor = _
	@Autowired var infoInterceptor: RequestInfoInterceptor = _

	@Autowired var contentNegotiationManager: ContentNegotiationManager = _

	/**
	 * If the interesting exception matches one of these exceptions then
	 * the given view name will be used instead of defaultView.
	 *
	 * Doesn't check subclasses, the exception class has to match exactly.
	 */
	@Required var viewMappings: JMap[String, String] = JHashMap[String, String]()

	override def resolveException(request: HttpServletRequest, response: HttpServletResponse, obj: Any, e: Exception): ModelAndView = {
		val interceptors = List(userInterceptor, infoInterceptor)
		for (interceptor <- interceptors) interceptor.preHandle(request, response, obj)

		doResolve(e, Some(request), Some(response)).noLayoutIf(ajax).toModelAndView
	}

	override def requestInfo: Option[RequestInfo] = RequestInfo.fromThread

	private def ajax = requestInfo.exists { _.ajax }

	/**
	 * Resolve an exception outside of a request. Doesn't return a model/view.
	 */
	def resolveException(e: Exception) { doResolve(e) }

	/**
	 * Simpler interface for ErrorController to delegate to, which is called when an exception
	 * happens beyond Spring's grasp.
	 */
	def doResolve(e: Throwable, request: Option[HttpServletRequest] = None, response: Option[HttpServletResponse] = None): Mav = {
		def loggedIn = requestInfo.exists { _.user.loggedIn }
		def isAjaxRequest = request.isDefined && ("XMLHttpRequest" == request.get.getHeader("X-Requested-With"))

		e match {
			// Handle unresolvable @PathVariables as a page not found (404). HFC-408
			case typeMismatch: TypeMismatchException => handle(new ItemNotFoundException(typeMismatch), request, response)

			// Handle request method not supported as a 405
			case methodNotSupported: HttpRequestMethodNotSupportedException => handle(new MethodNotSupportedException(methodNotSupported), request, response)

			// Handle missing servlet param exceptions as 400
			case missingParam: MissingServletRequestParameterException => handle(new ParameterMissingException(missingParam), request, response)

			// Handle missing request body
			case missingBody: HttpMessageNotReadableException => handle(new RequestBodyMissingException(missingBody), request, response)

			// TAB-411 also redirect to signin for submit permission denied if not logged in (and not ajax request)
			case permDenied: PermissionsError if !loggedIn && !isAjaxRequest && !request.exists { _.isJsonRequest } => RedirectToSignin()

			case e: IOException if ExceptionHandler.isClientAbortException(e) => Mav(null.asInstanceOf[String])

			// TAB-567 wrap MultipartException in UserError so it doesn't get logged as an error
			case uploadError: MultipartException => handle(new FileUploadException(uploadError), request, response)

			case exception: Throwable => handle(exception, request, response)
			case _ => handleNull
		}
	}

	/**
	 * Catch any exception in the given callback. Useful for wrapping some
	 * work that's done outside of a request, such as a scheduled task, because
	 * otherwise the exception will be only minimally logged by the scheduler.
	 */
	def reportExceptions[A](fn: => A): A =
		try fn
		catch { case throwable: Throwable => handle(throwable, None, None); throw throwable }

	private def handle(exception: Throwable, request: Option[HttpServletRequest], response: Option[HttpServletResponse]) = {
		val token = ExceptionTokens.newToken

		val interestingException = ExceptionUtils.getInterestingThrowable(exception, Array(classOf[ServletException]))

		if (logger.isDebugEnabled && interestingException != null) {
			logger.debug(s"Handling exception ${interestingException.getClass.getName} (${interestingException.getMessage})")
		}

		val mav = Mav(defaultView,
			"originalException" -> exception,
			"exception" -> interestingException,
			"token" -> token,
			"stackTrace" -> ExceptionHandler.renderStackTrace(interestingException))

		// handler will do logging, emailing
		try {
			exceptionHandler.exception(ExceptionContext(token, interestingException, request))
		} catch {
			// This is very bad and should never happen - but still try to avoid showing
			// a plain exception to the user.
			case e: Exception => logger.error("Exception handling exception!", e)
		}

		viewMappings.get(interestingException.getClass.getName) match {
			case view: String => mav.viewName = view
			case null => //keep defaultView
		}

		val httpStatus = interestingException match {
			case error: UserError => error.httpStatus
			case _ => HttpStatus.INTERNAL_SERVER_ERROR
		}

		val statusReason = interestingException match {
			case error: UserError => error.httpStatusReason
			case _ => httpStatus.getReasonPhrase
		}

		response.foreach { _.setStatus(httpStatus.value()) }

		request.foreach { request =>
			if (request.isJsonRequest) {
				mav.viewName = null
				mav.view = new JSONView(Map(
					"success" -> false,
					"status" -> statusReason.toLowerCase.replace(' ', '_'),
					"errors" -> Array(Map("message" -> interestingException.getMessage))
				))
			} else if (request.requestedUri.getPath.startsWith("/profiles/view/photo") && contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request)).asScala.exists(_.getType == "image")) {
				// FIXME this is a bit general, and would be confusing if you were downloading jpg of someone's submission
				response.foreach(_.addHeader("X-Error", interestingException.getMessage))

				mav.viewName = null
				mav.view = new RenderableFileView(DefaultPhoto)
			}
		}

		mav
	}

	private def handleNull = {
		logger.error("Unexpectedly tried to resolve a null exception!")
		Mav(defaultView)
	}

}

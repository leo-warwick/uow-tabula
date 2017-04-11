package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateExceptionHandler
import freemarker.template.TemplateException
import uk.ac.warwick.tabula.system.exceptions.ExceptionTokens
import freemarker.core.Environment
import java.io.Writer
import uk.ac.warwick.tabula.system.exceptions.ExceptionHandler
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.exceptions.ExceptionContext
import uk.ac.warwick.util.core.ExceptionUtils
import org.apache.tiles.TilesException

// An exception handler that delegates to Tabula's exception handler for logging and emailing
class CustomFreemarkerExceptionHandler extends TemplateExceptionHandler {
	lazy val handler: ExceptionHandler = Wire[ExceptionHandler]
	lazy val production: Boolean = Wire.property("${environment.production}").toBoolean

	def handleTemplateException(exception: TemplateException, env: Environment, out: Writer) {
		// Ignore Tiles errors, since they are just multiple errors
		if (ExceptionUtils.retrieveException(exception, classOf[TilesException]) == null) {
			val token = ExceptionTokens.newToken // TODO output the token to HTML
			val request = None // TODO would like to get current request

			handler.exception(ExceptionContext(token, exception, request))

			// On production, we just ignore errors and let the template carry on. We'll get the email anyway
			val delegate =
				if (production) TemplateExceptionHandler.IGNORE_HANDLER
				else TemplateExceptionHandler.HTML_DEBUG_HANDLER

			delegate.handleTemplateException(exception, env, out)
		}
	}
}
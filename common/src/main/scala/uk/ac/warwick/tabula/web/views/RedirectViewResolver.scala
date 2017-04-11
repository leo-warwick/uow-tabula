package uk.ac.warwick.tabula.web.views

import java.util.Locale
import scala.util.matching.Regex._
import scala.util.matching.Regex
import org.springframework.beans.factory.annotation.Required
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import uk.ac.warwick.tabula.helpers.Ordered

/**
 * Handles redirect: prefixed view names, using knowledge of the actual
 * request from Apache to redirect to the right location.
 */
class RedirectViewResolver extends ViewResolver with Ordered {

	val redirectPattern = new Regex("redirect:(/.*)")

	@Required
	var toplevelUrl: String = _

	def resolveViewName(viewName: String, locale: Locale): View = viewName match {
		case redirectPattern(urlPath) => new RedirectView(toplevelUrl + urlPath)
		case _ => null
	}

}
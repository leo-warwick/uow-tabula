package uk.ac.warwick.tabula.helpers

import javax.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.condition.{ConsumesRequestCondition, ProducesRequestCondition}
import uk.ac.warwick.util.web.Uri

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import HttpServletRequestUtils._
import org.springframework.web.util.UriComponentsBuilder

/**
 * Scala-style HttpServletRequest utilities. Adds the methods as implicit methods
 * on HttpServletRequest.
 */
trait HttpServletRequestUtils {
	class SuperHttpServletRequest(request: HttpServletRequest) {
		private def isResponseType(mediaTypes: MediaType*) =
			new ProducesRequestCondition((MediaType.TEXT_HTML +: mediaTypes).map(_.toString): _*).getMatchingCondition(request) match {
				case null => false
				case condition if condition.getExpressions.asScala.exists(_.getMediaType == MediaType.TEXT_HTML) => false
				case _ => true
			}

		def isJsonRequest: Boolean = {
			// Assume yes for the API
			if (requestedUri.getPath.startsWith("/api")) true
			else {
				if (new ConsumesRequestCondition("application/json").getMatchingCondition(request) != null) true
				else isResponseType(MediaType.APPLICATION_JSON, MediaType.parseMediaType("text/json"))
			}
		}

		def isAjaxRequest: Boolean = {
			val hasXHRHeader = request.getHeader(AjaxHeader) match {
				case "XMLHttpRequest" => true
				case _ => false
			}

			val hasAJAXParam = request.getParameter("ajax") match {
				case s: String => true
				case _ => false
			}

			hasXHRHeader || hasAJAXParam
		}

		def requestedUri: Uri = Uri.parse(request.getHeader(XRequestedUriHeader) match {
			case string: String => string
			case _ =>
				val builder = UriComponentsBuilder.fromUriString(request.getRequestURL.toString)
				for ((name, values) <- requestParameters; value <- values) yield builder.queryParam(name, value)
				builder.toUriString
		})

		def requestParameters: Map[String, Seq[String]] = {
			val params =
				for ((key, value) <- request.getParameterMap.asScala.toMap[Any, Any])
					yield (key, value) match {
						case (key: String, values: Array[String]) => (key, values.toSeq)
					}

			params.toMap
		}
	}

	implicit def HttpServletRequestToSuperHttpServletRequest(request: HttpServletRequest) = new SuperHttpServletRequest(request)
}

object HttpServletRequestUtils extends HttpServletRequestUtils {
	val AjaxHeader = "X-Requested-With"
	val XRequestedUriHeader = "X-Requested-Uri"
}

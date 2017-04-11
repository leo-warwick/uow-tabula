package uk.ac.warwick.tabula.web.views

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import org.springframework.http.HttpStatus
import uk.ac.warwick.tabula.JavaImports._

/**
	* View to be used when the parameters were valid but the request failed - e.g. when
	* the external service is at fault, the request passed validation but something
	* happened beyond Tabula's control with timetabling, Turnitin etc.
	*/
class JSONRequestFailedView(errors: Seq[Map[String, Any]]) extends JSONView(Map()) {

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType())
		response.setStatus(HttpStatus.PAYMENT_REQUIRED.value())

		val out = response.getWriter

		val json = Map(
			"success" -> false,
			"status" -> "request_failed",
			"errors" -> errors
		)

		objectMapper.writeValue(out, json)
	}

}

package uk.ac.warwick.tabula.web.views

import org.springframework.http.HttpStatus

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.validation.Errors
import org.springframework.context.MessageSource
import uk.ac.warwick.spring.Wire

class JSONErrorView(val errors: Errors, val additionalData: Map[String, _]) extends JSONView(Map()) {

	def this(errors: Errors) = this(errors, Map())

	var messageSource: MessageSource = Wire.auto[MessageSource]

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType())
		response.setStatus(HttpStatus.BAD_REQUEST.value())

		val out = response.getWriter
		val errorList = errors.getFieldErrors
		val errorMap = Map() ++ (errorList map (error => (error.getField, getMessage(error.getCode, error.getArguments: _*))))

		val globalErrors = errors.getGlobalErrors.map { error => GlobalError(error.getCode, getMessage(error.getCode, error.getArguments: _*)) }.toArray
		val fieldErrors = errors.getFieldErrors.map { error => FieldError(error.getCode, error.getField, getMessage(error.getCode, error.getArguments: _*)) }.toArray

		val errorJson = Map(
			"success" -> false,
			"status" -> "bad_request",
			"result" -> errorMap,
			"errors" -> (globalErrors ++ fieldErrors)
		)

		val finalJson = errorJson ++ additionalData

		objectMapper.writeValue(out, finalJson)
	}

	def getMessage(key: String, args: Object*): String = messageSource.getMessage(key, if (args == null) Array() else args.toArray, null)

	case class GlobalError(code: String, message: String)
	case class FieldError(code: String, field: String, message: String)

}

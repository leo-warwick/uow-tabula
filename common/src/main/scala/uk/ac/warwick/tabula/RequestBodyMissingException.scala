package uk.ac.warwick.tabula

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import uk.ac.warwick.tabula.system.exceptions.UserError

class RequestBodyMissingException(ex: HttpMessageNotReadableException) extends RuntimeException(ex.getMessage, ex) with UserError {
	override val httpStatus = HttpStatus.BAD_REQUEST
}

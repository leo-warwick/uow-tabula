package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.system.exceptions.UserError
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException

class ParameterMissingException(ex: MissingServletRequestParameterException) extends RuntimeException(ex.getMessage, ex) with UserError {
	override val httpStatus = HttpStatus.BAD_REQUEST
}
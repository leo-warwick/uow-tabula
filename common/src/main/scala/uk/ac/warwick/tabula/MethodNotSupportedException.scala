package uk.ac.warwick.tabula

import org.springframework.http.HttpStatus
import org.springframework.web.HttpRequestMethodNotSupportedException
import uk.ac.warwick.tabula.system.exceptions.UserError

class MethodNotSupportedException(ex: HttpRequestMethodNotSupportedException) extends RuntimeException(ex.getMessage, ex) with UserError {
	 override val httpStatus = HttpStatus.METHOD_NOT_ALLOWED
}

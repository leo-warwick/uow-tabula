package uk.ac.warwick.tabula

import org.springframework.http.HttpStatus
import uk.ac.warwick.tabula.system.exceptions.UserError

class RequestFailedException(message: String, exception: Throwable) extends RuntimeException(message, exception) with UserError {
	override val httpStatus = HttpStatus.PAYMENT_REQUIRED
	override def httpStatusReason = "Request Failed" // Override 'Payment Required'
}

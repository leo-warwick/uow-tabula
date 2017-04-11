package uk.ac.warwick.tabula.system.exceptions

import org.springframework.http.HttpStatus

/**
 * Declares an exception caused by some user error, which
 * shouldn't be logged as an error in the logs.
 */
trait UserError {
	val httpStatus: HttpStatus
	def httpStatusReason: String = httpStatus.getReasonPhrase
}
package uk.ac.warwick.tabula.api.commands

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.validation.Errors

@JsonAutoDetect
trait JsonApiRequest[A] extends Serializable {

	def copyTo(state: A, errors: Errors)

}

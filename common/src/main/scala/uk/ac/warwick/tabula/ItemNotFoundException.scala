package uk.ac.warwick.tabula

import org.springframework.http.HttpStatus
import uk.ac.warwick.tabula.system.exceptions.UserError

class ItemNotFoundException(val item: Any, val message: String) extends RuntimeException(message) with UserError {
  def this() = {
    this((), "Item not found")
  }

  def this(item: Any) = {
    this(item, "Item not found: " + item)
  }

  override val httpStatus = HttpStatus.NOT_FOUND
}

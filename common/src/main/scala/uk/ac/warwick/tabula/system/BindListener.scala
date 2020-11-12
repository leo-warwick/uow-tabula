package uk.ac.warwick.tabula.system

import org.springframework.validation.BindingResult
import scala.reflect._

trait BindListener {
  def onBind(result: BindingResult): Unit

  implicit class RichBindingResult(result: BindingResult) {
    def target[A: ClassTag]: Option[A] = result.getTarget match {
      case a: A => Option(a)
      case _ => None
    }
  }

}

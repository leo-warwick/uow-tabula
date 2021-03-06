package uk.ac.warwick.tabula.validators

import javax.validation.ConstraintValidator
import org.joda.time.ReadableInstant
import javax.validation.ConstraintValidatorContext
import org.joda.time.DateTime
import org.joda.time.Years

class WithinYearsValidatorForReadableInstant extends ConstraintValidator[WithinYears, ReadableInstant] {

  var maxPast: Int = _
  var maxFuture: Int = _

  override def initialize(annotation: WithinYears): Unit = {
    maxPast = annotation.maxPast()
    maxFuture = annotation.maxFuture()
  }

  override def isValid(v: ReadableInstant, context: ConstraintValidatorContext): Boolean = {
    val now = DateTime.now

    Option(v) match {
      case Some(value) if value.isAfter(now) => Years.yearsBetween(now, value).getYears <= maxFuture
      case Some(value) if value.isBefore(now) => Years.yearsBetween(value, now).getYears <= maxPast
      case Some(_) => true // exactly equal to current datetime
      case _ => true // null values are allowed
    }
  }

}
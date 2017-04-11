package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class DayOfWeekConverter extends TwoWayConverter[String, DayOfWeek] {

	override def convertRight(value: String): DayOfWeek =
		if (value.hasText) try { DayOfWeek(value.toInt) } catch { case e: NumberFormatException => null }
		else null

	override def convertLeft(day: DayOfWeek): String = Option(day).map { _.getAsInt.toString }.orNull
}
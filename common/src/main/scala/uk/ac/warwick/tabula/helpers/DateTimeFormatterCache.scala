package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

private[helpers] class DateTimeFormatterCache {
	private val map = JConcurrentMap[String, DateTimeFormatter]()
	def retrieve(pattern: String): DateTimeFormatter = map.getOrElseUpdate(pattern, DateTimeFormat.forPattern(pattern))
}
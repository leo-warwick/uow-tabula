package uk.ac.warwick.tabula.events

import org.slf4j.{Logger, LoggerFactory}

class SLF4JEventListener extends EventListener {
	import EventDescription._

	val logger: Logger = LoggerFactory.getLogger("uk.ac.warwick.tabula.AUDIT")

	override def beforeCommand(event: Event) {
		val s = generateMessage(event, "pre-event")
		logger.info(s.toString)
	}

	override def afterCommand(event: Event, returnValue: Any) {
		val s = generateMessage(event)
		logger.info(s.toString)
	}

	override def onException(event: Event, exception: Throwable) {
		val s = generateMessage(event, "failed-event")
		logger.info(s.toString)
	}

}

object EventDescription {
	val QUOTE = "\""
	val ESCQUOTE: String = "\\" + QUOTE

	def generateMessage(event: Event, eventStage: String = "event"): StringBuilder = {
		val s = new StringBuilder
		s ++= eventStage ++ "=" ++ event.name
		if (event.userId != null) {
			s ++= " user=" ++ userString(event.userId)
			if (event.realUserId != event.userId) {
				s ++= " realUser=" ++ userString(event.realUserId)
			}
		}
		describe(event, s)
		s
	}

	def userString(id: String): String = id match {
		case string: String => string
		case _ => "null"
	}

	// only supports DescriptionImpl
	def describe(event: Event, s: StringBuilder): Unit =
		for ((key, value) <- event.extra)
			s ++= " " ++ key ++ "=" ++ quote(stringOf(value))

	private def stringOf(obj: Any) = if (obj == null) "(null)" else obj.toString

	def quote(value: String): String = {
		if (value.contains(" ") || value.contains(QUOTE))
			QUOTE + value.replace(QUOTE, ESCQUOTE) + QUOTE
		else
			value
	}
}
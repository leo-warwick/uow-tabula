package uk.ac.warwick.tabula.events

import java.util.UUID
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.RequestInfo
import java.io.Serializable

/**
 * Event is a transient object created by the event listener to
 * track the information we need to log. The class that gets
 * saved to database is [[uk.ac.warwick.tabula.data.model.AuditEvent]].
 */
case class Event(
	val id: String,
	val name: String,
	val userId: String,
	val realUserId: String,
	val extra: Map[String, Any],
	val date: DateTime = new DateTime) extends Serializable

/**
 * Stores an Event with its stage (before, after, error).
 * This is mainly for serializing in maintenance mode, so that we can
 * read it back out
 */
case class EventAndStage(event: Event, stage: String) extends Serializable

object Event {
	def fromDescribable(describable: Describable[_]): Event = doFromDescribable(describable, None)
	def resultFromDescribable[A](describable: Describable[A], result: A, id: String): Event = doFromDescribable(describable, Some(result), id)

	private def doFromDescribable[A](describable: Describable[A], result: Option[A], id: String = null) = {
		val description = new DescriptionImpl
		result.map(r => describable.describeResult(description, r))
			.getOrElse(describable.describe(description))

		val (apparentId, realUserId) = getUser match {
			case Some(user) => (user.apparentId, user.realId)
			case None => (null, null)
		}
		val eventId = id match {
			case id: String => id
			case _ => UUID.randomUUID.toString
		}
		new Event(
			eventId,
			describable.eventName,
			apparentId,
			realUserId,
			description.allProperties.toMap)
	}

	private def getUser = RequestInfo.fromThread.map { _.user }
}
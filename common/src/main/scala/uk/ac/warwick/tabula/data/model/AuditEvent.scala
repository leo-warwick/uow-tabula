package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.events.Event
import uk.ac.warwick.tabula.data.model.AuditEvent._

object AuditEvent {
	type DataType = Map[String, Any]
	val BlankData = Map.empty[String, Any]
}

/**
 * Represents a single item in the audit trail.
 * Every event has a unique ID, but an action will usually have
 * multiple events relating to each stage of the action. eventStage
 * stores the stage as "before", "after", "error". Events for the
 * same action are joined by the eventId which they share.
 *
 * It might make more sense to have a separate class responsible
 * for holding a group of these, rather than reusing the same class
 * to hold a reference to its siblings (and itself).
 *
 * AuditEventService.getById will fetch events with the same eventId
 * and place them in the "related" property. This list includes this
 * event itself.
 */
case class AuditEvent(
	var id: Long = 0, // unique to this object
	var eventId: String = null, // shared between different stage of the same action
	var eventDate: DateTime = null,
	var eventType: String = null,
	var eventStage: String = null, // before, after, error

	// The actual user who did the event.
	var userId: String = null,

	// Who appeared to do the event. If you're masqueraded as X, this field will contain X
	// while userId will contain your ID.
	var masqueradeUserId: String = null,

	//todo convert to/from json
	var data: String = null,

	/** this is set manually */
	var parsedData: Option[DataType] = None,

	// list of other related events (with same eventId) manually set by DAO
	@transient var related: Seq[AuditEvent] = Nil) extends Identifiable {

	/** Collects up all the parsed data maps for all related events. */
	def relatedParsedData: Seq[DataType] = related.flatMap { _.parsedData }

	/**
	 * Joins the JSON data for all the related audit events into one map. It is a simple
	 * merge by key - if any events re-use the same key, only one will get returned.
	 */
	def combinedParsedData: Map[String, Any] = relatedParsedData.foldLeft(BlankData) { (combined, map) => map ++ combined }

	def assignmentId: Option[String] = stringProperty("assignment")
	def submissionId: Option[String] = stringProperty("submission")
	def submissionIsNoteworthy: Option[String] = stringProperty("submissionIsNoteworthy")
	def submissionIds: Seq[String] = stringListProperty("submissions")
	def feedbackIds: Seq[String] = stringListProperty("feedbacks")
	def students: Seq[String] = stringListProperty("students")
	def studentUsercodes: Seq[String] = stringListProperty("studentUsercodes")
	def attachments: Seq[String] = stringListProperty("attachments")

	/** Was there an "error" stage, indicating an exception was thrown? */
	def hadError: Boolean = findStage("error").isDefined
	def isIncomplete: Boolean = findStage("after").isEmpty

	def findStage(stage: String): Option[AuditEvent] = related.find(_.eventStage == stage)
	def findBeforeStage: Option[AuditEvent] = findStage("before")

	/** Returns whether any of the events have a property. */
	def hasProperty(name: String): Boolean = stringProperty(name).isDefined

	/** Returns whether any of the events have a property with this string value. */
	def hasProperty(name: String, value: String): Boolean = stringProperty(name).contains(value)

	/**
	 * Looks among the JSON data for a string value under this name.
	 * It checks the related events' JSON too, returning the first value
	 * found.
	 */
	private def stringProperty(name: String): Option[String] =
		relatedParsedData
			.flatMap { _.get(name) }
			.map { _.toString }.headOption

	private def stringListProperty(name: String): Seq[String] =
		relatedParsedData
			.flatMap { _.get(name) }
			.flatMap { _.asInstanceOf[Seq[String]] }

	/**
	 * Convert to an Event object (losing the stage information). There's usually
	 * little reason to do this except maybe during testing.
	 */
	def toEvent = Event(
		eventId,
		eventType,
		masqueradeUserId,
		userId,
		parsedData.getOrElse(Map.empty),
		eventDate)

	override def toString = s"AuditEvent[id=$id, eventId=$eventId, eventDate=$eventDate, eventType=$eventType, eventStage=$eventStage, userId=$userId, masqueradeUserId=$masqueradeUserId, data=$data]"

}
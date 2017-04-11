package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.SubmissionService

/** Class to expose bean properties via constructor.
 *
 *  It currently takes a single entity, as we're currently only interested in
 *  simple relations between agents (users) and entities (objects),
 *  eg. Student does Submission
 *  In future, perhaps we'd extend this to take a collection of entities
 *  for more complex interactions.
 */
class Activity[A](
	val id: String,
	val title: String,
	val date: DateTime,
	val priority: Double,
	val agent: User,
	val url: String,
	val urlTitle: String,
	val verb: String,
	val message: String,
	val entity: A,
	target: AnyRef=null
) {

	// Expose entity type for Freemarker
	def getEntityType: String = entity.getClass.getSimpleName
}

/** Companion object offers apply method to construct new Activities,
 *  drawing its data from other types.
 */
object Activity {
	var userLookup: UserLookupService = Wire[UserLookupService]
	var submissionService: SubmissionService = Wire[SubmissionService]

	// given an AuditEvent...
	def apply(event: AuditEvent): Option[Activity[Any]] = {
		event.eventType match {
			case "SubmitAssignment" if event.hasProperty("submission") =>
				val submission = submissionService.getSubmission(event.submissionId getOrElse "")

				val title = if (submission.isDefined) {
						"New submission"
					} else {
						"New submission (since deleted)"
					}

				val entity = submission.getOrElse(Nil)
				val date = event.eventDate
				val agent = userLookup.getUserByUserId(event.userId)
				Some(new Activity[Any](null, title, date, 0, agent, null, null, "submit", "", entity))
			case _ =>
				None
		}
	}
}

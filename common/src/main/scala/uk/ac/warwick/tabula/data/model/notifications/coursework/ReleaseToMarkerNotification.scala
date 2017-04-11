package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object ReleaseToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("ReleaseToMarker")
class ReleaseToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with Logging
	with AllCompletedActionRequiredNotification {

	def this(markerNumber: Int) {
		this()
		whichMarker.value = markerNumber
	}

	def workflowVerb: String = Option(assignment.markingWorkflow).map { markingWorkflow =>
		whichMarker.value match {
			case 1 => assignment.markingWorkflow.firstMarkerVerb
			case 2 => assignment.markingWorkflow.secondMarkerVerb.getOrElse {
				logger.warn("Attempted to read secondMarkerVerb for workflow without second marker")
				""
			}
		}
	}.getOrElse {
		logger.warn("Attempted to read workflowVerb for assignment without workflow")
		""
	}

	@transient val whichMarker = IntSetting("marker", 1)

	def verb = "released"
	def assignment: Assignment = target.entity

	def title: String = "%s: Submissions for \"%s\" have been released for marking".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel(ReleaseToMarkerNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numReleasedFeedbacks" -> items.size,
			"workflowVerb" -> workflowVerb
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"$workflowVerb these submissions"

	priority = Warning

}


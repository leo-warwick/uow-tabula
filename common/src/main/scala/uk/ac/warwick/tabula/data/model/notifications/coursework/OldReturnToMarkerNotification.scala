package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, NotificationWithTarget, SingleRecipientNotification, UserIdRecipientNotification, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object OldReturnToMarkerNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/return_to_marker_notification.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("ReturnToMarker")
class OldReturnToMarkerNotification
  extends NotificationWithTarget[MarkerFeedback, Assignment]
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with Logging
    with AllCompletedActionRequiredNotification {

  def this(markerNumber: Int, commentValue: String) {
    this()
    whichMarker.value = markerNumber
    comment.value = commentValue
  }

  def workflowVerb: String = Option(assignment.markingWorkflow).map { markingWorkflow =>

    def noVerb = {
      logger.warn("Attempted to read secondMarkerVerb for workflow without second marker")
      ""
    }

    whichMarker.value match {
      case 1 => assignment.markingWorkflow.firstMarkerVerb
      case 2 => assignment.markingWorkflow.secondMarkerVerb.getOrElse {
        noVerb
      }
      case 3 => assignment.markingWorkflow.thirdMarkerVerb.getOrElse {
        noVerb
      }
    }
  }.getOrElse {
    logger.warn("Attempted to read workflowVerb for assignment without workflow")
    ""
  }

  @transient val whichMarker = IntSetting("marker", 1)
  @transient val comment = StringSetting("comment", "")

  def verb = "returned"

  def assignment: Assignment = target.entity

  def title = s"${assignment.module.code.toUpperCase}: Submissions for ${assignment.name} have been returned to you"

  def content = FreemarkerModel(OldReturnToMarkerNotification.templateLocation,
    Map(
      "assignment" -> assignment,
      "numReleasedFeedbacks" -> items.size,
      "workflowVerb" -> workflowVerb,
      "comment" -> comment.value
    ))

  def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

  def urlTitle = s"$workflowVerb this feedback"

  priority = Warning

}


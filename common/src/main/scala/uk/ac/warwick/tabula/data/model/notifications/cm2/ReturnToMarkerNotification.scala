package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, SingleRecipientNotification, UserIdRecipientNotification, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object ReturnToMarkerNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/return_to_marker_notification.ftl"
  val batchTemplateLocation = "/WEB-INF/freemarker/emails/return_to_marker_notification_batch.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("CM2ReturnToMarker")
class ReturnToMarkerNotification
  extends BatchedNotificationWithTarget[MarkerFeedback, Assignment, ReturnToMarkerNotification](ReturnToMarkerBatchedNotificationHandler)
    with SingleRecipientNotification
    with UserIdRecipientNotification
    with AutowiringUserLookupComponent
    with Logging
    with AllCompletedActionRequiredNotification {

  def this(stage: MarkingWorkflowStage, commentValue: String) = {
    this()
    whichMarker.value = stage.name
    comment.value = commentValue
  }

  def workflowVerb: String = whichMarker.value match {
    case MarkingWorkflowStage(s) => s.verb
    case _ => MarkingWorkflowStage.DefaultVerb
  }

  @transient val whichMarker = StringSetting("stage", "")
  @transient val comment = StringSetting("comment", "")

  def verb = "returned"

  def assignment: Assignment = target.entity

  def title = s"${assignment.module.code.toUpperCase}: Submissions for ${assignment.name} have been returned to you"

  def content = FreemarkerModel(ReturnToMarkerNotification.templateLocation,
    Map(
      "assignment" -> assignment,
      "feedbackDeadlineDate" -> assignment.feedbackDeadline.map(dateOnlyFormatter.print),
      "numReleasedFeedbacks" -> items.size,
      "comment" -> comment.value
    ))

  def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

  def urlTitle = s"$workflowVerb this feedback"

  priority = Warning

}

object ReturnToMarkerBatchedNotificationHandler extends BatchedNotificationHandler[ReturnToMarkerNotification] {
  override def titleForBatchInternal(notifications: Seq[ReturnToMarkerNotification], user: User): String = {
    val assignments = notifications.map(_.assignment).distinct

    if (assignments.size == 1) notifications.head.titleFor(user)
    else s"Submissions for ${assignments.size} assignments have been returned to you"
  }

  override def contentForBatchInternal(notifications: Seq[ReturnToMarkerNotification]): FreemarkerModel =
    FreemarkerModel(ReturnToMarkerNotification.batchTemplateLocation, Map(
      "notifications" -> notifications.groupBy(_.assignment)
        .toSeq.sortBy(_._2.map(_.created).min)
        .map { case (assignment, batch) =>
          Map(
            "assignment" -> assignment,
            "feedbackDeadlineDate" -> assignment.feedbackDeadline.map(DateFormats.NotificationDateOnly.print),
            "numReleasedFeedbacks" -> batch.map(_.items.size()).sum,
            "comments" -> batch.flatMap(_.comment.value.maybeText),
          )
        }
    ))

  override def urlForBatchInternal(notifications: Seq[ReturnToMarkerNotification], user: User): String =
    Routes.marker()

  override def urlTitleForBatchInternal(notifications: Seq[ReturnToMarkerNotification]): String =
    "view assignments for marking"
}

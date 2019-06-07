package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object ExtensionRevokedNotification {
  val templateLocation = "/WEB-INF/freemarker/emails/revoke_manual_extension.ftl"
}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue("ExtensionRevoked")
class ExtensionRevokedNotification extends Notification[Assignment, Unit]
  with SingleItemNotification[Assignment]
  with SingleRecipientNotification
  with UniversityIdOrUserIdRecipientNotification
  with AutowiringUserLookupComponent
  with MyWarwickNotification {

  priority = Warning

  def assignment: Assignment = item.entity

  def verb = "revoke"

  def title: String = "%s: Your extended deadline for \"%s\" has been revoked".format(assignment.module.code.toUpperCase, assignment.name)

  def url: String = Routes.assignment(assignment)

  def urlTitle = "view the assignment"

  def content = FreemarkerModel(ExtensionRevokedNotification.templateLocation, Map(
    "assignment" -> assignment,
    "module" -> assignment.module,
    "user" -> recipient,
    "originalAssignmentDate" -> Option(assignment.submissionDeadline(recipient)).map(dateTimeFormatter.print).orNull
  ))
}
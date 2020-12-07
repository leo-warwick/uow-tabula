package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

abstract class ExtensionStudentNotification extends ExtensionNotification with SingleRecipientNotification {

  self: MyWarwickDiscriminator =>

  def recipient: User = student

  def url: String = Routes.assignment(assignment)

  def template: String

  def content = FreemarkerModel(template, Map(
    "assignment" -> assignment,
    "module" -> assignment.module,
    "user" -> recipient,
    "extension" -> extension,
    "newExpiryDate" -> dateTimeFormatter.print(extension.expiryDate.getOrElse(assignment.closeDate)),
    "originalAssignmentDate" -> Option(assignment.submissionDeadline(recipient)).map(dateTimeFormatter.print).orNull
  ))
}

@Entity
@Proxy
@DiscriminatorValue("ExtensionChanged")
class ExtensionChangedNotification extends ExtensionStudentNotification with MyWarwickActivity {
  def verb = "updated"

  def title: String = titlePrefix + "Your extended deadline for \"%s\" has changed".format(assignment.name)

  def template = "/WEB-INF/freemarker/emails/modified_manual_extension.ftl"

  def urlTitle = "view the modified deadline"
}

@Entity
@Proxy
@DiscriminatorValue("ExtensionGranted")
class ExtensionGrantedNotification extends ExtensionStudentNotification with MyWarwickActivity {
  def verb = "grant"

  def title: String = titlePrefix + "Your deadline for \"%s\" has been extended".format(assignment.name)

  def template = "/WEB-INF/freemarker/emails/new_manual_extension.ftl"

  def urlTitle = "view your new deadline"
}

@Entity
@Proxy
@DiscriminatorValue("ExtensionRequestApproved")
class ExtensionRequestApprovedNotification extends ExtensionStudentNotification with MyWarwickActivity {
  def verb = "approve"

  def title: String = {
    val verbed = if (extension.expiryDateAdjusted) "adjusted and approved" else "approved"

    titlePrefix + "Your extension request for \"%s\" has been %s".format(assignment.name, verbed)
  }

  def template = "/WEB-INF/freemarker/emails/extension_request_approved.ftl"

  def urlTitle = "view your extension"
}

@Entity
@Proxy
@DiscriminatorValue("ExtensionRequestRejected")
class ExtensionRequestRejectedNotification extends ExtensionStudentNotification with MyWarwickNotification {
  def verb = "reject"

  def title: String = titlePrefix + "Your extension request for \"%s\" has been rejected".format(assignment.name)

  def template = "/WEB-INF/freemarker/emails/extension_request_rejected.ftl"

  def urlTitle = "view the assignment deadline"

  priority = NotificationPriority.Warning
}

@Entity
@Proxy
@DiscriminatorValue("ExtensionRequestMoreInfo")
class ExtensionRequestMoreInfo extends ExtensionStudentNotification with MyWarwickNotification {
  def verb = "request"

  def title: String = titlePrefix + "More information is required in order to review your extension request for \"%s\"".format(assignment.name)

  def template = "/WEB-INF/freemarker/emails/extension_info_requested.ftl"

  def urlTitle = "view your extension request"

  override def url: String = Routes.extensionRequest(assignment)

  priority = NotificationPriority.Warning
}
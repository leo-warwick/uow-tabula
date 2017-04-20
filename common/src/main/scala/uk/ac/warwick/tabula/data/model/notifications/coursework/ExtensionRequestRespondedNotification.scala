package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity}
import uk.ac.warwick.userlookup.User


abstract class ExtensionRequestRespondedNotification(val verbed: String) extends ExtensionNotification
	with MyWarwickActivity {

	def verb = "respond"

	def title: String = titlePrefix + "Extension request by %s for \"%s\" was %s".format(student.getFullName, assignment.name, verbed)

	def url: String = Routes.admin.assignment.extension.expandrow(assignment, student.getUserId)
	def urlTitle = "review this extension request"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/responded_extension_request.ftl", Map(
		"studentName" -> student.getFullName,
		"agentName" -> agent.getFullName,
		"verbed" -> verbed,
		"assignment" -> assignment,
		"path" ->  url
	))

	def recipients: Seq[User] = assignment.module.adminDepartment.extensionManagers.users.filterNot(_ == agent)

}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedApprove")
class ExtensionRequestRespondedApproveNotification extends ExtensionRequestRespondedNotification("approved") {}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedReject")
class ExtensionRequestRespondedRejectNotification extends ExtensionRequestRespondedNotification("rejected") {}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedMoreInfo")
class ExtensionRequestRespondedMoreInfoNotification extends ExtensionRequestRespondedNotification("returned for more information") {}
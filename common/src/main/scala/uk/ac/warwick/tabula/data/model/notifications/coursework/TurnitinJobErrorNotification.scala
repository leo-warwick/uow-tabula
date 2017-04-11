package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="TurnitinJobError")
class TurnitinJobErrorNotification
		extends TurnitinReportNotification
		with SingleRecipientNotification {

	def title: String = "%s: The Turnitin check for \"%s\" has not completed successfully".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/turnitinjobfailed.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"path" -> url
	))

	def recipient: User = agent
}

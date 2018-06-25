package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("UAMAuditNotification")
class UAMAuditNotification extends Notification[Department, Unit] with MyWarwickNotification {

	@transient
	val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_email.ftl"

	def departments: Seq[Department] = entities

	def verb: String = "view"

	def title: String = s"Tabula Users Audit ${DateTime.now().year()}"

	def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
		"departments" -> departments,
		"userAccessManager" -> agent,
		"deadline" -> DateTime.parse(s"${DateTime.now().year()}-08-31T00:00")
	))

	def url: String = "url to sitebuilder form"

	def urlTitle: String = "Tabula User Access Manager Confirmation Form"

	@transient
	def recipients: Seq[User] = Seq(agent)
}

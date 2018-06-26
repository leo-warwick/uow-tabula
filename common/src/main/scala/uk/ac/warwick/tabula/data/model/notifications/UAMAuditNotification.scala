package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.{AcademicYear, DateFormats}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

object UAMAuditNotification {

	object Deadline {
		val permissionConfirmation: LocalDate = AcademicYear
			.forDate(new DateTime())
			.next
			.termsAndVacations
			.filter(_.isTerm)
			.head
			.firstDay
			.minusWeeks(1)

		val roleConfirmation: LocalDate = permissionConfirmation.minusWeeks(4)
	}

}

@Entity
@DiscriminatorValue("UAMAuditNotification")
class UAMAuditNotification extends Notification[Department, Unit] with MyWarwickNotification {

	@transient
	val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_email.ftl"

	def departments: Seq[Department] = entities

	def verb: String = "view"

	def title: String = s"Tabula Users Audit ${this.created.getYear}"

	def url: String = "https://warwick.ac.uk/services/its/servicessupport/web/tabula/uamconfirmation/"

	def urlTitle: String = title

	def content: FreemarkerModel = FreemarkerModel(templateLocation, Map(
		"departments" -> departments,
		"userAccessManager" -> agent.getFullName,
		"permissionConfirmation" -> UAMAuditNotification.Deadline.permissionConfirmation.toString(DateFormats.NotificationDateOnlyPattern),
		"roleConfirmation" -> UAMAuditNotification.Deadline.roleConfirmation.toString(DateFormats.NotificationDateOnlyPattern),
		"url" -> url,
		"urlTitle" -> urlTitle
	))

	@transient
	def recipients: Seq[User] = Seq(agent)
}

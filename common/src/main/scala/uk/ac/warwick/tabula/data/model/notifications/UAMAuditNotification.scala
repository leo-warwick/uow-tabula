package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ProfileService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object UAMAuditNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/uam_audit_email.ftl"
}

@Entity
@DiscriminatorValue("UAMAuditNotification")
class UAMAuditNotification extends Notification[Department, Unit]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	var profileService: ProfileService = Wire[ProfileService]

	// these departments have the same UAM
	def departments: Seq[Department] = entities

	// there is only one recipient, which is the uam
	def userAccessManger: User = departments.head.grantedRoles.asScala.head.users.users.head

	def verb: String = "view"

	def title: String = "some good title regarding"

	def content: FreemarkerModel = FreemarkerModel(UAMAuditNotification.templateLocation, Map(
		"departments" -> this.departments,
		"userAccessManger" -> this.userAccessManger,
		"deadline" -> DateTime.parse(DateTime.now().year() + "-08-31T00:00")
	))

	def url: String = "url to sitebuilder form"

	def urlTitle: String = "???"

	@transient
	def recipients: Seq[User] = Seq(userAccessManger)
}

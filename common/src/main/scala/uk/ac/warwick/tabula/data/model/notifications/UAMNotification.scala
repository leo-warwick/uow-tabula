package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.UserAccessMgrRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ProfileService}
import uk.ac.warwick.userlookup.User

object UAMNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/uam_email.ftl"
}

@Entity
@DiscriminatorValue("UAMNotification")
class UAMNotification extends Notification[User, Unit]
	with SingleItemNotification[User]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	@transient
	var profileService: ProfileService = Wire[ProfileService]

	@transient
	var permissionsService: PermissionsService = Wire[PermissionsService]

	def user: User = item.entity

	def getUAMs: Seq[User] = {
		profileService.getMemberByUser(this.user)
			.get
			.affiliatedDepartments
			.flatMap { dept =>
				permissionsService.getAllGrantedRolesFor(dept)
					.filter(_.roleDefinition == UserAccessMgrRoleDefinition)
					.flatMap(_.users.users)
			}
	}

	def verb: String = ???

	def title: String = ???

	def content: FreemarkerModel = ???

	def url: String = ???

	/**
		* URL title will be used to generate the links in notifications
		*
		* Activities will use - <a href=${url}>${urlTitle}</a>  (first letter of url title will be converted to upper case)
		* Emails will use - Please visit [${url}] to ${urlTitle}
		*/
	def urlTitle: String = ???

	def recipients: Seq[User] = ???
}

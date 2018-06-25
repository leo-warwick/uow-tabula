package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.DiscriminatorValue
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object UAMNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/uam_email.ftl"
}

@DiscriminatorValue("UAMNotification")
class UAMNotification extends Notification[User, Unit]
	with SingleItemNotification[User]
	with AutowiringUserLookupComponent
	with MyWarwickNotification {

	override def verb: String = ???

	override def title: String = ???

	override def content: FreemarkerModel = ???

	override def url: String = ???

	/**
		* URL title will be used to generate the links in notifications
		*
		* Activities will use - <a href=${url}>${urlTitle}</a>  (first letter of url title will be converted to upper case)
		* Emails will use - Please visit [${url}] to ${urlTitle}
		*/
	override def urlTitle: String = ???

	override def recipients: Seq[User] = ???
}

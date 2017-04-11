package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, NotificationWithTarget, SingleItemNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User


abstract class ExtensionNotification extends NotificationWithTarget[Extension, Assignment]
	with SingleItemNotification[Extension]
	with AutowiringUserLookupComponent {

	def extension: Extension = item.entity
	def assignment: Assignment = target.entity
	def student: User = userLookup.getUserByUserId(extension.usercode)

	def titlePrefix: String = target.entity.module.code.toUpperCase + ": "
}

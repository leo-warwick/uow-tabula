package uk.ac.warwick.tabula.commands.home

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Activity, Notification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.NotificationServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object DismissNotificationCommand {

	def apply(notifications: Seq[Notification[_,_]], dismiss: Boolean, user: User) =
		new DismissNotificationCommandInternal(notifications, dismiss, user)
			with ComposableCommand[Seq[Activity[_]]]
			with DismissNotificationCommandPermissions
			with DismissNotificationCommandDescription
			with DismissNotificationCommandValidation
}

abstract class DismissNotificationCommandInternal(val notifications: Seq[Notification[_,_]], val dismiss: Boolean, val user: User)
		extends CommandInternal[Seq[Activity[_]]] with DismissNotificationCommandState with NotificationServiceComponent{

	def applyInternal(): Seq[Activity[Any]] = {
		if (dismiss) {
			notifications.foreach(_.dismiss(user))
		} else {
			notifications.foreach(_.unDismiss(user))
		}
		notificationService.update(notifications, user)
		notifications.flatMap(notificationService.toActivity)
	}
}

trait DismissNotificationCommandValidation extends SelfValidating {
	self: DismissNotificationCommandState =>
	def validate(errors: Errors) {

	}
}

trait DismissNotificationCommandState {
	val notifications: Seq[Notification[_,_]]
	val dismiss: Boolean
	val user: User
}

trait DismissNotificationCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DismissNotificationCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAll(Permissions.Notification.Dismiss, notifications)
	}
}

trait DismissNotificationCommandDescription extends Describable[Seq[Activity[_]]] {
	self: DismissNotificationCommandState =>
	def describe(d: Description) {
		d.notifications(notifications)
		d.property("dismiss", dismiss)
	}
}
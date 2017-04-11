package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionRevokedNotification

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.{ComposableCommand, Notifies}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.userlookup.User

object DeleteExtensionCommand {
	def apply(mod: Module, ass: Assignment, student: User, sub: CurrentUser) =
		new DeleteExtensionCommandInternal(mod, ass, student, sub)
			with ComposableCommand[Extension]
			with DeleteExtensionCommandPermissions
			with ModifyExtensionCommandDescription
			with DeleteExtensionCommandNotification
			with AutowiringUserLookupComponent
			with HibernateExtensionPersistenceComponent
}

class DeleteExtensionCommandInternal(mod: Module, ass: Assignment, student: User, sub: CurrentUser)
	extends ModifyExtensionCommand(mod, ass, student, sub) with ModifyExtensionCommandState {

	self: ExtensionPersistenceComponent with UserLookupComponent =>

	extension = assignment.findExtension(student.getUserId)
		.getOrElse({ throw new IllegalStateException("Cannot delete a missing extension") })

	def applyInternal(): Extension = transactional() {
		extension._state = ExtensionState.Revoked
		assignment.extensions.remove(extension)
		extension.attachments.asScala.foreach(delete(_))
		delete(extension)
		extension
	}
}

trait DeleteExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: ModifyExtensionCommandState =>

	def emit(extension: Extension): Seq[ExtensionRevokedNotification] = {
		val notification = Notification.init(new ExtensionRevokedNotification, submitter.apparentUser, Seq(extension.assignment))
		notification.recipientUniversityId = extension.usercode
		Seq(notification)
	}
}


trait DeleteExtensionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyExtensionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Extension.Delete, assignment)
	}
}

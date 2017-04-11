package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment

object DownloadExtensionAttachmentCommand {
	def apply(extension: Extension, filename: String) = new DownloadExtensionAttachmentCommandInternal(extension, filename)
		with ComposableCommand[Option[RenderableAttachment]]
		with DownloadExtensionAttachmentPermissions
		with DownloadExtensionAttachmentDescription
}

class DownloadExtensionAttachmentCommandInternal(val extension: Extension, val filename: String)
	extends CommandInternal[Option[RenderableAttachment]] with DownloadExtensionAttachmentState  {

	def applyInternal(): Option[RenderableAttachment] = {
		val allAttachments = extension.nonEmptyAttachments
		allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))
	}
}

trait DownloadExtensionAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadExtensionAttachmentState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Extension.Read, extension)
	}
}

trait DownloadExtensionAttachmentDescription extends Describable[Option[RenderableAttachment]] {
	self: DownloadExtensionAttachmentState =>

	override def describe(d: Description) {
		d.assignment(extension.assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description, result: Option[RenderableAttachment]) {
		d.property("fileFound", result.isDefined)
	}
}

trait DownloadExtensionAttachmentState {
	val extension: Extension
	val filename: String
}
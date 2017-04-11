package uk.ac.warwick.tabula.commands.coursework.assignments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import beans.BeanProperty
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions._

class DownloadSupportingFilesCommand(
		val module: Module,
		val assignment: Assignment,
		val extension: Extension,
		val filename: String)
		extends Command[Option[RenderableFile]] with ReadOnly{

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Extension.Read, extension)

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal(): Option[RenderableAttachment] = {
		val allAttachments = extension.nonEmptyAttachments
		val attachment = allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = attachment.isDefined
		if (callback != null) {
			attachment.map { callback(_) }
		}
		attachment
	}

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}
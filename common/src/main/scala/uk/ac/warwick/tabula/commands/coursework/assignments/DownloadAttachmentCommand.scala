package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.permissions._

class DownloadAttachmentCommand(
		val module: Module,
		val assignment: Assignment,
		val submission: Submission,
		val student: Option[Member])
		extends Command[Option[RenderableFile]] with ReadOnly {

	mustBeLinked(mandatory(assignment), mandatory(module))

	student match {
		case Some(student: StudentMember) => PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Submission.Read, submission),
				CheckablePermission(Permissions.Submission.Read, student))
		)
		case _ => PermissionCheck(Permissions.Submission.Read, submission)
	}

	var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal(): Option[RenderableAttachment] = {
		val attachment = submission.allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = attachment.isDefined
		if (callback != null) {
			attachment.foreach { callback }
		}
		attachment
	}

	override def describe(d: Description): Unit = {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}
package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}

class DownloadAttachmentCommand(
		val assignment: Assignment,
		val submission: Submission,
		val student: Option[Member])
		extends Command[Option[RenderableFile]] with ReadOnly {

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
			attachment.map { callback(_) }
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
package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver._

import scala.collection.JavaConversions.asScalaBuffer

class DownloadFeedbackCommand(val module: Module, val assignment: Assignment, val feedback: Feedback, val student: Option[Member])
	extends Command[Option[RenderableFile]] with ReadOnly {

	notDeleted(assignment)
	mustBeLinked(assignment, module)

	student match {
		case Some(student: StudentMember) => PermissionCheckAny(
			Seq(CheckablePermission(Permissions.AssignmentFeedback.Read, feedback),
				CheckablePermission(Permissions.AssignmentFeedback.Read, student))
		)
		case _ => PermissionCheck(Permissions.AssignmentFeedback.Read, feedback)
	}

	var zip: ZipService = Wire.auto[ZipService]
	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	var filename: String = _
	var students: JList[String] = JArrayList()

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def applyInternal(): Option[RenderableFile] = {
		filename match {
			case filename: String if filename.hasText =>
				feedback.attachments.find(_.name == filename).map(new RenderableAttachment(_))
			case _ => Some(zipped(feedback))
		}
	}

	private def zipped(feedback: Feedback) = student match {
		case Some(student: StudentMember) => zip.getFeedbackZip(feedback)
		case _ => zip.getFeedbackZip(feedback)
	}

	override def describe(d: Description): Unit = {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description, result: Option[RenderableFile]) {
		d.property("fileFound", result.isDefined)
	}

}
package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkerFeedback, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}

class AdminGetSingleFeedbackCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[RenderableFile] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, feedback)

	var zipService: ZipService = Wire.auto[ZipService]

	override def applyInternal(): RenderableFile = zipService.getFeedbackZip(feedback)

	override def describe(d: Description): Unit = d.feedback(feedback).properties(
		"studentId" -> feedback.studentIdentifier,
		"attachmentCount" -> feedback.attachments.size)
}

class AdminGetSingleFeedbackFileCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[Option[RenderableFile]] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, feedback)

	var filename: String = _

	private var fileFound: Boolean = _

	def applyInternal(): Option[RenderableAttachment] = {
		val attachment = Option(new RenderableAttachment(feedback.attachments.get(0)))
		fileFound = attachment.isDefined
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



class AdminGetSingleMarkerFeedbackCommand(module: Module, assignment: Assignment, markerFeedback: MarkerFeedback) extends Command[RenderableFile] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)

	var zipService: ZipService = Wire.auto[ZipService]

	override def applyInternal(): RenderableFile = zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback))

	override def describe(d: Description): Unit = d.feedback(markerFeedback.feedback).properties(
		"studentId" -> markerFeedback.feedback.studentIdentifier,
		"attachmentCount" -> markerFeedback.attachments.size)
}
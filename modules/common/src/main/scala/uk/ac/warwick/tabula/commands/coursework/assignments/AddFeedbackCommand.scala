package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.services.AutowiringFeedbackServiceComponent
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Notifies, Description, UploadedFile}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.Errors

class AddFeedbackCommand(module: Module, assignment: Assignment, marker: User, currentUser: CurrentUser)
	extends UploadFeedbackCommand[Seq[Feedback]](module, assignment, marker) with Notifies[Seq[Feedback], Feedback]
		with AutowiringFeedbackServiceComponent {

	PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)

	override def applyInternal(): Seq[Feedback] = transactional() {

		def saveFeedback(uniNumber: String, file: UploadedFile):Option[Feedback] = {
			val feedback = assignment.findFeedback(uniNumber).getOrElse({
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = marker.getUserId
				newFeedback.universityId = uniNumber
				newFeedback.released = false
				newFeedback.createdDate = DateTime.now
				newFeedback.updatedDate = DateTime.now
				newFeedback
			})

			val newAttachments = feedback.addAttachments(file.attached)

			if (newAttachments.nonEmpty) {
				feedbackService.saveOrUpdate(feedback)
				Some(feedback)
			} else {
				None
			}
		}

		// TODO should really do this in a more general place, like a save listener for Feedback objects
		val updatedFeedback = if (items != null && !items.isEmpty) {
			val feedbacks = items.map { (item) =>
				val feedback = saveFeedback(item.uniNumber, item.file)
				feedback.foreach(zipService.invalidateIndividualFeedbackZip)
				feedback
			}.toList.flatten
			zipService.invalidateFeedbackZip(assignment)
			feedbacks
		} else {
			val feedback = saveFeedback(uniNumber, file)
			// delete feedback zip for this assignment, since it'll now be different.
			zipService.invalidateFeedbackZip(assignment)
			feedback.toList
		}

		updatedFeedback
	}

	override def validateExisting(item: FeedbackItem, errors: Errors) {
		// warn if feedback for this student is already uploaded
		assignment.feedbacks.find { feedback => feedback.universityId == item.uniNumber && feedback.hasAttachments } match {
			case Some(feedback) =>
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				item.isPublished = feedback.released
				checkForDuplicateFiles(item, feedback)
			case None =>
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: Feedback){
		case class duplicatePair(attached:FileAttachment, feedback:FileAttachment)

		val attachmentNames = item.file.attached.map(_.name)
		val withSameName = for (
			attached <- item.file.attached;
			feedback <- feedback.attachments
			if attached.name == feedback.name
		) yield new duplicatePair(attached, feedback)

		item.duplicateFileNames = withSameName.filterNot(p => p.attached.isDataEqual(p.feedback)).map(_.attached.name).toSet
		item.ignoredFileNames = withSameName.map(_.attached.name).toSet -- item.duplicateFileNames
		item.isModified =  (attachmentNames -- item.ignoredFileNames).nonEmpty
	}

	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(items.map { _.uniNumber })

	override def describeResult(d: Description, feedbacks: Seq[Feedback]) = {
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
		 .fileAttachments(feedbacks.flatMap { _.attachments })
		 .properties("feedback" -> feedbacks.map { _.id })
	}

	def emit(updatedFeedback: Seq[Feedback]) = {
		updatedFeedback.filter(_.released).flatMap { feedback => HibernateHelpers.initialiseAndUnproxy(feedback) match {
			case assignmentFeedback: AssignmentFeedback =>
				Option(Notification.init(new FeedbackChangeNotification, marker, assignmentFeedback, assignment))
			case _ =>
				None
		}}
	}
}
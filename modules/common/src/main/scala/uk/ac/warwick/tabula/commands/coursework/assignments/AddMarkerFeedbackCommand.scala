package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.AutowiringFeedbackServiceComponent
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{UploadedFile, Description}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.Errors


class AddMarkerFeedbackCommand(module: Module, assignment:Assignment, marker: User, val submitter: CurrentUser)
	extends UploadFeedbackCommand[List[MarkerFeedback]](module, assignment, marker) with CanProxy
		with AutowiringFeedbackServiceComponent {

	PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
	if (submitter.apparentUser != marker) {
		PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
	}

	// list to contain feedback files that are not for a student you should be marking
	var invalidStudents: JList[FeedbackItem] = LazyLists.create()
	// list to contain feedback files that are  for a student that has already been completed
	var markedStudents: JList[FeedbackItem] = LazyLists.create()

	val submissions = assignment.getMarkersSubmissions(marker)

	def processStudents() {
		val markedSubmissions = submissions.filter{ submission =>
			val markerFeedback =  assignment.getMarkerFeedbackForCurrentPosition(submission.universityId, marker)
			markerFeedback match {
				case Some(f) if f.state != MarkingCompleted => true
				case _ => false
			}
		}
		val universityIds = submissions.map(_.universityId)
		val markedIds = markedSubmissions.map(_.universityId)
		invalidStudents = items.filter(item => !universityIds.contains(item.uniNumber))
		markedStudents = items.filter(item => !markedIds.contains(item.uniNumber))
		items = items.toList.diff(invalidStudents.toList).diff(markedStudents.toList)
	}

	private def saveMarkerFeedback(uniNumber: String, file: UploadedFile) = {
		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.find(_.universityId == uniNumber).getOrElse({
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = marker.getUserId
			newFeedback.universityId = uniNumber
			newFeedback.released = false
			newFeedback.createdDate = DateTime.now
			newFeedback
		})

		val markerFeedback:MarkerFeedback = parentFeedback.getCurrentWorkflowFeedback match {
			case None => throw new IllegalArgumentException
			case Some(mf) => mf
		}

		for (attachment <- file.attached){
			// if an attachment with the same name as this one exists then delete it
			val duplicateAttachment = markerFeedback.attachments.find(_.name == attachment.name)
			duplicateAttachment.foreach(markerFeedback.removeAttachment)
			markerFeedback.addAttachment(attachment)
		}

		markerFeedback.state = InProgress
		parentFeedback.updatedDate = DateTime.now

		feedbackService.saveOrUpdate(parentFeedback)
		feedbackService.save(markerFeedback)

		markerFeedback
	}

	override def applyInternal(): List[MarkerFeedback] = transactional() {
		if (items != null && !items.isEmpty) {
			val markerFeedbacks = items.map { (item) =>
				val feedback = saveMarkerFeedback(item.uniNumber, item.file)
				feedback
			}
			markerFeedbacks.toList
		} else {
			val markerFeedbacks = saveMarkerFeedback(uniNumber, file)
			List(markerFeedbacks)
		}
	}

	override def validateExisting(item: FeedbackItem, errors: Errors) {

		// warn if feedback for this student is already uploaded
		assignment.feedbacks.find { feedback => feedback.universityId == item.uniNumber } flatMap { _.getCurrentWorkflowFeedback } match {
			case Some(markerFeedback) if markerFeedback.hasFeedback =>
				// set warning flag for existing feedback and check if any existing files will be overwritten
				item.submissionExists = true
				checkForDuplicateFiles(item, markerFeedback)
			case Some(markerFeedback) =>
			case _ => errors.reject("markingWorkflow.feedback.finalised", "No more feedback can be added")
		}
	}

	private def checkForDuplicateFiles(item: FeedbackItem, feedback: MarkerFeedback){
		val attachedFiles = item.file.attachedFileNames.toSet
		val feedbackFiles = feedback.attachments.map(file => file.getName).toSet
		item.duplicateFileNames = attachedFiles & feedbackFiles
	}

	def describe(d: Description){
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
	}

	override def describeResult(d: Description, feedbacks: List[MarkerFeedback]) = {
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
		 .fileAttachments(feedbacks.flatMap { _.attachments })
		 .properties("feedback" -> feedbacks.map { _.id })
	}
}

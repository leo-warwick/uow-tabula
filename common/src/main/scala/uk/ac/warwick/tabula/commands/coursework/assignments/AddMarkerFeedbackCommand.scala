package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
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
	extends UploadFeedbackCommand[List[MarkerFeedback]](module, assignment, marker) with CanProxy {

	PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
	if(submitter.apparentUser != marker) {
		PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
	}

	// list to contain feedback files that are not for a student you should be marking
	var invalidStudents: JList[FeedbackItem] = LazyLists.create()
	// list to contain feedback files that are  for a student that has already been completed
	var markedStudents: JList[FeedbackItem] = LazyLists.create()

	val submissions: Seq[Submission] = assignment.getMarkersSubmissions(marker)

	def processStudents() {
		val markedSubmissions = submissions.filter{ submission =>
			val markerFeedback =  assignment.getMarkerFeedbackForCurrentPosition(submission.usercode, marker)
			markerFeedback match {
				case Some(f) if f.state != MarkingCompleted => true
				case _ => false
			}
		}
		val allUsercodes = submissions.map(_.usercode)
		val markedUsercodes = markedSubmissions.map(_.usercode)

		invalidStudents = items.filter(item => {
			val usercode = item.student.map(_.getUserId).getOrElse("")
			!allUsercodes.contains(usercode)
		})

		markedStudents = items.filter(item => {
			val usercode = item.student.map(_.getUserId).getOrElse("")
			!markedUsercodes.contains(usercode)
		})

		items = items.toList.diff(invalidStudents.toList).diff(markedStudents.toList)
	}

	private def saveMarkerFeedback(student: User, file: UploadedFile) = {
		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.find(_.usercode == student.getUserId).getOrElse({
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = marker.getUserId
			newFeedback.usercode = student.getUserId
			newFeedback._universityId = student.getWarwickId
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

		session.saveOrUpdate(parentFeedback)
		session.saveOrUpdate(markerFeedback)

		markerFeedback
	}

	override def applyInternal(): List[MarkerFeedback] = transactional() {
		val markerFeedbacks = for(item <- items; student <- item.student) yield saveMarkerFeedback(student, item.file)
		markerFeedbacks.toList
	}

	override def validateExisting(item: FeedbackItem, errors: Errors) {

		val usercode = item.student.map(_.getUserId)
		val feedback = usercode.flatMap(u => assignment.feedbacks.find(_.usercode == u))

		// warn if feedback for this student is already uploaded
		feedback flatMap { _.getCurrentWorkflowFeedback } match {
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
		 .studentUsercodes(items.flatMap(_.student.map(_.getUserId)))
	}

	override def describeResult(d: Description, feedbacks: List[MarkerFeedback]): Unit = {
		d.assignment(assignment)
		 .studentIds(items.map { _.uniNumber })
		 .studentUsercodes(items.flatMap(_.student.map(_.getUserId)))
		 .fileAttachments(feedbacks.flatMap { _.attachments })
		 .properties("feedback" -> feedbacks.map { _.id })
	}
}

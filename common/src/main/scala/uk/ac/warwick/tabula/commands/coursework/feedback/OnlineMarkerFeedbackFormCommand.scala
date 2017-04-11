package uk.ac.warwick.tabula.commands.coursework.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.AutowiringSavedFormValueDaoComponent
import uk.ac.warwick.tabula.data.model.MarkingState.InProgress
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

object OnlineMarkerFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: User, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OnlineMarkerFeedbackFormCommand(module, assignment, student, marker, submitter, gradeGenerator)
			with ComposableCommand[MarkerFeedback]
			with MarkerFeedbackStateCopy
			with OnlineFeedbackFormPermissions
			with CopyFromFormFields
			with WriteToFormFields
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with AutowiringProfileServiceComponent
			with OnlineFeedbackFormDescription[MarkerFeedback] {
			override lazy val eventName = "OnlineMarkerFeedback"
		}
}

abstract class OnlineMarkerFeedbackFormCommand(
	module: Module,
	assignment: Assignment,
	student: User,
	marker: User,
	val submitter: CurrentUser,
	gradeGenerator: GeneratesGradesFromMarks
)	extends AbstractOnlineFeedbackFormCommand(module, assignment, student, marker, gradeGenerator)
	with CommandInternal[MarkerFeedback] with Appliable[MarkerFeedback] {

	self: FeedbackServiceComponent with ZipServiceComponent with MarkerFeedbackStateCopy =>

	def markerFeedback: Option[MarkerFeedback] = assignment.getMarkerFeedbackForCurrentPosition(student.getUserId, marker)
	def allMarkerFeedbacks: Seq[MarkerFeedback] = assignment.getAllMarkerFeedbacks(student.getUserId, marker)

	if (markerFeedback.isDefined) copyState(markerFeedback)

	def applyInternal(): MarkerFeedback = {
		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.asScala.find(_.usercode == student.getUserId).getOrElse({
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = marker.getUserId
			newFeedback.usercode = student.getUserId
			newFeedback._universityId = student.getWarwickId
			newFeedback.released = false
			newFeedback.createdDate = DateTime.now
			newFeedback
		})

		// see if marker feedback already exists - if not create one
		val markerFeedback: MarkerFeedback = parentFeedback.getCurrentWorkflowFeedback match {
			case None => throw new IllegalArgumentException
			case Some(mf) => mf
		}

		copyTo(markerFeedback)
		markerFeedback.state = InProgress

		parentFeedback.updatedDate = DateTime.now

		feedbackService.saveOrUpdate(parentFeedback)
		feedbackService.save(markerFeedback)
		markerFeedback
	}

}

trait MarkerFeedbackStateCopy {

	self: OnlineFeedbackState with OnlineFeedbackStudentState with CopyFromFormFields with WriteToFormFields
		with FileAttachmentServiceComponent =>

	var rejectionComments: String = _

	/*
		If there is a marker feedback then use the specified copy function to copy it's state to the form object
		if not then set up blank field values
	*/
	def copyState(markerFeedback: Option[MarkerFeedback], copyFunction: MarkerFeedback => Unit): Unit = markerFeedback match {
		case Some(f) => copyFunction(f)
		case None =>
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
	}

	// when we dont specify a copy function use the one in this trait
	def copyState(markerFeedback: Option[MarkerFeedback]): Unit = copyState(markerFeedback, copyFrom)

	def copyFrom(markerFeedback: MarkerFeedback) {

		copyFormFields(markerFeedback)

		// mark and grade
		if (assignment.collectMarks){
			mark = markerFeedback.mark match {
				case Some(m) => m.toString
				case None => ""
			}
			grade = markerFeedback.grade.getOrElse("")
		}

		if(markerFeedback.rejectionComments.hasText) {
			rejectionComments = markerFeedback.rejectionComments
		}

		// get attachments
		attachedFiles = markerFeedback.attachments
	}

	def copyTo(markerFeedback: MarkerFeedback) {

		saveFormFields(markerFeedback)

		// save mark and grade
		if (assignment.collectMarks) {
			markerFeedback.mark = mark.maybeText.map(_.toInt)
			markerFeedback.grade = grade.maybeText
		}


		if(rejectionComments.hasText) {
			markerFeedback.rejectionComments = rejectionComments
		}


		// save attachments
		if (markerFeedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(markerFeedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			val filesToReplicate = filesToKeep -- existingFiles
			fileAttachmentService.deleteAttachments(filesToRemove)
			markerFeedback.attachments = JArrayList[FileAttachment](filesToKeep)
			val replicatedFiles = filesToReplicate.map ( _.duplicate() )
			replicatedFiles.foreach(markerFeedback.addAttachment)
		}
		markerFeedback.addAttachments(file.attached.asScala)
	}
}

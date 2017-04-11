package uk.ac.warwick.tabula.commands.coursework.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser

import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{StringFormValue, SavedFormValue, FormValue}
import uk.ac.warwick.tabula.data.{AutowiringSavedFormValueDaoComponent, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User


object OnlineFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: User, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OnlineFeedbackFormCommand(module, assignment, student, marker, submitter, gradeGenerator)
			with ComposableCommand[Feedback]
			with OnlineFeedbackFormPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with AutowiringProfileServiceComponent
			with OnlineFeedbackFormDescription[Feedback] {
			override lazy val eventName = "OnlineFeedback"
		}
}

abstract class OnlineFeedbackFormCommand(
	module: Module,
	assignment: Assignment,
	student: User,
	marker: User,
	val submitter: CurrentUser,
	gradeGenerator: GeneratesGradesFromMarks
) extends AbstractOnlineFeedbackFormCommand(module, assignment, student, marker, gradeGenerator) with CommandInternal[Feedback] with Appliable[Feedback] {

	self: FeedbackServiceComponent with SavedFormValueDaoComponent with FileAttachmentServiceComponent with ZipServiceComponent =>

	def feedback: Option[Feedback] = assignment.findFeedback(student.getUserId)

	feedback match {
		case Some(f) => copyFrom(f)
		case None =>
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
	}

	def applyInternal(): Feedback = {

		val feedback = assignment.findFeedback(student.getUserId).getOrElse({
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = marker.getUserId
			newFeedback.usercode = student.getUserId
			newFeedback._universityId = student.getWarwickId
			newFeedback.released = false
			newFeedback.createdDate = DateTime.now
			newFeedback
		})

		copyTo(feedback)

		// if we are updating existing feedback then invalidate any cached feedback zips
		if(feedback.id != null) {
			zipService.invalidateIndividualFeedbackZip(feedback)
		}

		feedback.updatedDate = DateTime.now

		feedbackService.saveOrUpdate(feedback)
		feedback
	}

	def copyFrom(feedback: Feedback) {
		// get custom field values
		fields = {
			val pairs = assignment.feedbackFields.map { field =>
				val currentValue = feedback.customFormValues.asScala.find(_.name == field.name)
				val formValue = currentValue match {
					case Some(initialValue) => field.populatedFormValue(initialValue)
					case None => field.blankFormValue
				}
				field.id -> formValue
			}
			Map(pairs: _*).asJava
		}

		// mark and grade
		if (assignment.collectMarks){
			mark = feedback.actualMark match {
				case Some(m) => m.toString
				case None => ""
			}
			grade = feedback.actualGrade.getOrElse("")
		}

		// get attachments
		attachedFiles = feedback.attachments
	}

	def copyTo(feedback: Feedback) {
		// save custom fields
		feedback.customFormValues.addAll(fields.asScala.map {
			case (_, formValue) =>

				def newValue = {
					val newValue = new SavedFormValue()
					newValue.name = formValue.field.name
					newValue.feedback = feedback
					newValue
				}

				// Don't send brand new feedback to the DAO or we'll get a TransientObjectException
				val savedFormValue = if (feedback.id == null) {
					newValue
				} else {
					savedFormValueDao.get(formValue.field, feedback).getOrElse(newValue)
				}

				formValue.persist(savedFormValue)
				savedFormValue
		}.toSet[SavedFormValue].asJava)

		// save mark and grade
		if (assignment.collectMarks) {
			feedback.actualMark = mark.maybeText.map(_.toInt)
			feedback.actualGrade = grade.maybeText
		}

		// save attachments
		if (feedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(feedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			fileAttachmentService.deleteAttachments(filesToRemove)
			feedback.attachments = JArrayList[FileAttachment](filesToKeep)
		}
		feedback.addAttachments(file.attached.asScala)
	}

}



trait CopyFromFormFields {

	self: OnlineFeedbackStudentState with SavedFormValueDaoComponent =>

	def copyFormFields(markerFeedback: MarkerFeedback){
		// get custom field values
		fields = {
			val pairs = assignment.feedbackFields.map { field =>
				val currentValue = markerFeedback.customFormValues.asScala.find(_.name == field.name)
				val formValue = currentValue match {
					case Some(initialValue) => field.populatedFormValue(initialValue)
					case None => field.blankFormValue
				}
				field.id -> formValue
			}
			Map(pairs: _*).asJava
		}
	}

}

trait WriteToFormFields {

	self: OnlineFeedbackStudentState with SavedFormValueDaoComponent =>

	def saveFormFields(markerFeedback: MarkerFeedback) {
		// save custom fields
		markerFeedback.customFormValues = fields.asScala.map {
			case (_, formValue) =>

				def newValue = {
					val newValue = new SavedFormValue()
					newValue.name = formValue.field.name
					newValue.markerFeedback = markerFeedback
					newValue
				}

				// Don't send brand new feedback to the DAO or we'll get a TransientObjectException
				val savedFormValue = if (markerFeedback.id == null) {
					newValue
				} else {
					savedFormValueDao.get(formValue.field, markerFeedback).getOrElse(newValue)
				}

				formValue.persist(savedFormValue)
				savedFormValue
		}.toSet[SavedFormValue].asJava
	}

}

trait OnlineFeedbackFormPermissions extends RequiresPermissionsChecking {

	self: OnlineFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait OnlineFeedbackStudentState {
	val student: User
	val assignment: Assignment

	var mark: String = _
	var grade: String = _
	var fields: JMap[String, FormValue] = _
	var file:UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	private def fieldHasVaue = fields.asScala.exists{ case (_, value: StringFormValue) => value.value.hasText }
	private def hasFile = Option(attachedFiles).exists(!_.isEmpty) || Option(file).exists(!_.attachedOrEmpty.isEmpty)

	def hasContent: Boolean = mark.hasText || grade.hasText || hasFile || fieldHasVaue
}

trait OnlineFeedbackFormDescription[A] extends Describable[A] {

	this: OnlineFeedbackState with OnlineFeedbackStudentState =>

	def describe(d: Description) {
		d.studentIds(Option(student.getWarwickId).toSeq)
		d.studentUsercodes(student.getUserId)
		d.assignment(assignment)
	}
}
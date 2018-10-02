package uk.ac.warwick.tabula.commands.coursework.assignments

import javax.validation.constraints.{Max, Min}
import org.hibernate.validator.constraints.Length
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{CommentField, FileField, MarkerSelectField, WordCountField}
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.services.ZipService

import scala.collection.JavaConverters._

/**
 * Bound as the value of a Map on a parent form object, to store multiple sets of
 * assignment properties.
 *
 * Currently relies on having a default empty constructor for lazy map populating to work,
 * so if you need to add a constructor then any command that has this as a Map value will need
 * to instantiate some kind of lazy map factory that knows how to create them (see Commons Collections LazyMap).
 */
class SharedAssignmentPropertiesForm extends SharedAssignmentProperties {
	// set this as the default for this form, because in the AddAssignmentsCommand
	// we're linking assignments in from SITS.
	restrictSubmissions = true
}


/**
 * Contains all the fields that could be collectively assigned to a group of assignments,
 * so that we can set options in one go.
 */
trait SharedAssignmentProperties extends BooleanAssignmentProperties with FindAssignmentFields {

	@Min(0)
	var wordCountMin: JInteger = _
	@Max(Assignment.MaximumWordCount)
	var wordCountMax: JInteger = _
	@Length(max = 600)
	var wordCountConventions: String = "Exclude any bibliography or appendices."

	// linked feedback template (optional)
	var feedbackTemplate: FeedbackTemplate = _
	// if we change a feedback template we may need to invalidate existing zips
	var zipService: ZipService = Wire.auto[ZipService]

	var markingWorkflow: CM2MarkingWorkflow = _

	@Min(1)
	@Max(Assignment.MaximumFileAttachments)
	var fileAttachmentLimit: Int = 1

	val maxFileAttachments: Int = 20

	val invalidAttachmentPattern = """.*[\*\\/:\?"<>\|\%].*"""

	var fileAttachmentTypes: JList[String] = JArrayList()

	@Min(0)
	var individualFileSizeLimit: JInteger = _

	/**
	 * This isn't actually a property on Assignment, it's one of the default fields added
	 * to all Assignments. When the forms become customisable this will be replaced with
	 * a full blown field editor.
	 */
	@Length(max = 2000)
	var comment: String = _

	def validateShared(errors: Errors) {
		if(fileAttachmentTypes.asScala.mkString("").matches(invalidAttachmentPattern)){
			errors.rejectValue("fileAttachmentTypes", "attachment.invalidChars")
		}

		// implicitly fix missing bounds
		(Option(wordCountMin), Option(wordCountMax)) match {
			case (Some(min), Some(max)) if max <= min => errors.rejectValue("wordCountMax", "assignment.wordCount.outOfRange")
			case (Some(min), None) => wordCountMax = Assignment.MaximumWordCount
			case (None, Some(max)) => wordCountMin = 0
			case _ => // It's All Good
		}
	}

	def copySharedTo(assignment: Assignment) {
		copyBooleansTo(assignment)

		assignment.feedbackTemplate = feedbackTemplate
		if (assignment.id != null) // this is an edit
			zipService.invalidateSubmissionZip(assignment)
		assignment.cm2MarkingWorkflow = markingWorkflow

		for (field <- findCommentField(assignment)) field.value = comment
		for (file <- findFileField(assignment)) {
			file.attachmentLimit = fileAttachmentLimit
			file.attachmentTypes = fileAttachmentTypes.asScala
			file.individualFileSizeLimit = individualFileSizeLimit
		}

		if (wordCountMin == null && wordCountMax == null) {
			findWordCountField(assignment).foreach{ wordCountField =>
				wordCountField.max = null
				wordCountField.min = null
			}
		} else {
			val wordCount = findWordCountField(assignment).getOrElse{
				val newField = new WordCountField()
				newField.name = Assignment.defaultWordCountName
				assignment.addField(newField)
				newField
			}
			wordCount.min = wordCountMin
			wordCount.max = wordCountMax
			wordCount.conventions = wordCountConventions
		}
	}

	def copySharedFrom(assignment: Assignment) {
		openEnded = assignment.openEnded
		collectMarks = assignment.collectMarks
		collectSubmissions = assignment.collectSubmissions
		restrictSubmissions = assignment.restrictSubmissions
		allowLateSubmissions = assignment.allowLateSubmissions
		allowResubmission = assignment.allowResubmission
		displayPlagiarismNotice = assignment.displayPlagiarismNotice
		allowExtensions = assignment.allowExtensions
		extensionAttachmentMandatory = assignment.extensionAttachmentMandatory
		allowExtensionsAfterCloseDate = assignment.allowExtensionsAfterCloseDate
		summative = assignment.summative
		dissertation = assignment.dissertation
		publishFeedback = assignment.publishFeedback
		feedbackTemplate = assignment.feedbackTemplate
		markingWorkflow = assignment.cm2MarkingWorkflow
		includeInFeedbackReportWithoutSubmissions = assignment.includeInFeedbackReportWithoutSubmissions
		automaticallyReleaseToMarkers = assignment.automaticallyReleaseToMarkers
		automaticallySubmitToTurnitin = assignment.automaticallySubmitToTurnitin

		for (field <- findCommentField(assignment)) comment = field.value
		for (file <- findFileField(assignment)) {
			fileAttachmentLimit = file.attachmentLimit
			fileAttachmentTypes = file.attachmentTypes.asJava
			individualFileSizeLimit = file.individualFileSizeLimit
		}

		for (wordCount <- findWordCountField(assignment)) {
			wordCountMin = wordCount.min
			wordCountMax = wordCount.max
			wordCountConventions = wordCount.conventions
		}
	}


}


trait FindAssignmentFields {

	protected def findFileField(assignment: Assignment): Option[FileField] =
		assignment.findFieldOfType[FileField](Assignment.defaultUploadName)

	protected def findMarkerSelectField(assignment: Assignment): Option[MarkerSelectField] =
		assignment.findFieldOfType[MarkerSelectField](Assignment.defaultMarkerSelectorName)

	/**Find the standard free-text field if it exists */
	protected def findCommentField(assignment: Assignment): Option[CommentField] =
		assignment.findFieldOfType[CommentField](Assignment.defaultCommentFieldName)

	protected def findWordCountField(assignment: Assignment): Option[WordCountField] = {
		assignment.findFieldOfType[WordCountField](Assignment.defaultWordCountName)
	}
}
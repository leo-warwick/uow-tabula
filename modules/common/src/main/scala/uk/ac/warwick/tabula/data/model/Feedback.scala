package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{AccessType, BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{FormattedHtml, SavedFormValue}
import uk.ac.warwick.tabula.permissions.PermissionsTarget

import scala.collection.JavaConversions._


trait FeedbackAttachments {

	// Do not remove
	// Should be import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
	import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

	var attachments: JList[FileAttachment]
	def addAttachment(attachment: FileAttachment)

	def hasAttachments: Boolean = !attachments.isEmpty

	def mostRecentAttachmentUpload =
		if (attachments.isEmpty) null
		else attachments.maxBy { _.dateUploaded }.dateUploaded

	/* Adds new attachments to the feedback. Ignores feedback that has already been uploaded and overwrites attachments
	   with the same name as exiting attachments. Returns the attachments that wern't ignored. */
	def addAttachments(fileAttachments: Seq[FileAttachment]) : Seq[FileAttachment] = fileAttachments.filter { a =>
		val isIdentical = attachments.exists(f => f.name == a.name && f.isDataEqual(a))
		if (!isIdentical) {
			// if an attachment with the same name as this one exists then replace it
			val duplicateAttachment = attachments.find(_.name == a.name)
			duplicateAttachment.foreach(removeAttachment)
			addAttachment(a)
		}
		!isIdentical
	}

	def removeAttachment(attachment: FileAttachment) = {
		attachment.feedback = null
		attachment.markerFeedback = null
		attachments.remove(attachment)
	}

	def clearAttachments() {
		for(attachment <- attachments){
			attachment.feedback = null
		}
		attachments.clear()
	}
}

@Entity @AccessType("field")
class Feedback extends GeneratedId with FeedbackAttachments with PermissionsTarget with ToEntityReference with FormattedHtml {

	type Entity = Feedback

	def this(universityId: String) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(fetch = FetchType.LAZY, cascade=Array(PERSIST, MERGE), optional = false)
	var assignment: Assignment = _

	def permissionsParents = Option(assignment).toStream

	var uploaderId: String = _

	@Column(name = "uploaded_date")
	var createdDate: DateTime = new DateTime

	@Column(name = "updated_date")
	@NotNull
	var updatedDate: DateTime = new DateTime

	var universityId: String = _

	var released: JBoolean = false

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingPrompt: Option[Boolean] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingHelpful: Option[Boolean] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var actualMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var agreedMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var actualGrade: Option[String] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var agreedGrade: Option[String] = None

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "first_marker_feedback")
	var firstMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "second_marker_feedback")
	var secondMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "third_marker_feedback")
	var thirdMarkerFeedback: MarkerFeedback = _


	def getFeedbackPosition(markerFeedback: MarkerFeedback) : FeedbackPosition = {
		if(markerFeedback == firstMarkerFeedback) FirstFeedback
		else if (markerFeedback == secondMarkerFeedback) SecondFeedback
		else if (markerFeedback == thirdMarkerFeedback) ThirdFeedback
		else throw new IllegalArgumentException
	}

	// Returns None if marking is completed for the current workflow or if no workflow exists - i.e. not in the middle of a workflow
	def getCurrentWorkflowFeedbackPosition: Option[FeedbackPosition] = {

		def markingCompleted(workflow: MarkingWorkflow) = {
			(workflow.hasThirdMarker && thirdMarkerFeedback != null && thirdMarkerFeedback.state == MarkingState.MarkingCompleted) ||
			(!workflow.hasThirdMarker && workflow.hasSecondMarker && secondMarkerFeedback != null && secondMarkerFeedback.state == MarkingState.MarkingCompleted) ||
			(!workflow.hasThirdMarker && !workflow.hasSecondMarker && firstMarkerFeedback != null && firstMarkerFeedback.state == MarkingState.MarkingCompleted)
		}

		Option(assignment.markingWorkflow)
			.filterNot(markingCompleted)
			.map { workflow =>
				if (workflow.hasThirdMarker && secondMarkerFeedback != null && secondMarkerFeedback.state == MarkingState.MarkingCompleted)
					ThirdFeedback
				else if (workflow.hasSecondMarker && secondMarkerFeedback != null && secondMarkerFeedback.state == MarkingState.Rejected)
					FirstFeedback
				else if (workflow.hasSecondMarker && firstMarkerFeedback != null && firstMarkerFeedback.state == MarkingState.MarkingCompleted)
					SecondFeedback
				else
					FirstFeedback
			}
	}

	def getCurrentWorkflowFeedback: Option[MarkerFeedback] = {
		getCurrentWorkflowFeedbackPosition match {
			case Some(FirstFeedback) => Option(retrieveFirstMarkerFeedback)
			case Some(SecondFeedback) => Option(retrieveSecondMarkerFeedback)
			case Some(ThirdFeedback) => Option(retrieveThirdMarkerFeedback)
			case _ => None
		}
	}

	@Column(name = "released_date")
	var releasedDate: DateTime = _

	@OneToMany(mappedBy = "feedback", cascade = Array(ALL))
	val customFormValues: JSet[SavedFormValue] = JHashSet()

	def clearCustomFormValues(): Unit = {
		customFormValues.foreach { v =>
			v.feedback = null
		}
		customFormValues.clear()
	}

	// FormValue containing the per-user online feedback comment
	def commentsFormValue = customFormValues.find(_.name == Assignment.defaultFeedbackTextFieldName)

	def comments: Option[String] = commentsFormValue.map(_.value)

	def commentsFormattedHtml: String = formattedHtml(comments)
	
	
	// Getters for marker feedback either return the marker feedback or create a new empty one if none exist
	def retrieveFirstMarkerFeedback:MarkerFeedback = {
		Option(firstMarkerFeedback).getOrElse({
			firstMarkerFeedback = new MarkerFeedback(this)
			firstMarkerFeedback
		})
	}

	def retrieveSecondMarkerFeedback:MarkerFeedback = {
		Option(secondMarkerFeedback).getOrElse({
			secondMarkerFeedback = new MarkerFeedback(this)
			secondMarkerFeedback
		})
	}

	def retrieveThirdMarkerFeedback:MarkerFeedback = {
		Option(thirdMarkerFeedback).getOrElse({
			thirdMarkerFeedback = new MarkerFeedback(this)
			thirdMarkerFeedback
		})
	}

	// The current workflow position isn't None so this must be a placeholder
	def isPlaceholder = getCurrentWorkflowFeedbackPosition.isDefined

	def hasMarkOrGrade = hasMark || hasGrade

	def hasMark: Boolean = actualMark.isDefined

	def hasGrade: Boolean = actualGrade.isDefined

	// TODO in some other places we also check that the string value hasText. Be consistent?
	def hasOnlineFeedback: Boolean = commentsFormValue.isDefined

	def getAllMarkerFeedback: Seq[MarkerFeedback] = Seq(firstMarkerFeedback, secondMarkerFeedback, thirdMarkerFeedback)

	def getAllCompletedMarkerFeedback: Seq[MarkerFeedback] = Seq(firstMarkerFeedback, secondMarkerFeedback, thirdMarkerFeedback)
		.filter(_ != null)
		.filter(_.state == MarkingState.MarkingCompleted)

	def hasGenericFeedback: Boolean = Option(assignment.genericFeedback).isDefined

	/**
	 * Returns the released flag of this feedback,
	 * OR false if unset.
	 */
	def checkedReleased: Boolean = Option(released) match {
		case Some(bool) => bool
		case None => false
	}

	@OneToMany(mappedBy = "feedback", fetch = FetchType.LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var attachments: JList[FileAttachment] = JArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedback = this
		attachments.add(attachment)
	}

	/**
	 * Whether ratings are being collected for this feedback.
	 * Doesn't take into account whether the ratings feature is enabled, so you
	 * need to check that separately.
	 */
	def collectRatings: Boolean = assignment.module.adminDepartment.collectFeedbackRatings

	/**
	 * Whether marks are being collected for this feedback.
	 * Doesn't take into account whether the marks feature is enabled, so you
	 * need to check that separately.
	 */
	def collectMarks: Boolean = assignment.collectMarks

	override def toEntityReference = new FeedbackEntityReference().put(this)
}

object Feedback {
	val PublishDeadlineInWorkingDays = 20
}

object FeedbackPosition {
	def getPreviousPosition(position: Option[FeedbackPosition]): Option[FeedbackPosition] = position match {
		case Some(FirstFeedback) => None
		case Some(SecondFeedback) => Option(FirstFeedback)
		case Some(ThirdFeedback) => Option(SecondFeedback)
		case None => Option(ThirdFeedback)
	}
}

sealed trait FeedbackPosition
case object  FirstFeedback extends FeedbackPosition { val description = "First marker's feedback" }
case object  SecondFeedback extends FeedbackPosition { val description = "Second marker's feedback" }
case object  ThirdFeedback extends FeedbackPosition { val description = "Third marker's feedback" }

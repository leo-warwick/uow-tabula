package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{AcademicYear, JavaImports}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{FormattedHtml, SavedFormValue}
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage}
import uk.ac.warwick.tabula.permissions.PermissionsTarget

import scala.collection.JavaConverters._

trait FeedbackAttachments {

	// Do not remove
	// Should be import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
	import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

	var attachments: JList[FileAttachment]
	def addAttachment(attachment: FileAttachment)

	def hasAttachments: Boolean = !attachments.isEmpty

	def mostRecentAttachmentUpload: DateTime =
		if (attachments.isEmpty) null
		else attachments.asScala.maxBy { _.dateUploaded }.dateUploaded

	/* Adds new attachments to the feedback. Ignores feedback that has already been uploaded and overwrites attachments
	   with the same name as exiting attachments. Returns the attachments that wern't ignored. */
	def addAttachments(fileAttachments: Seq[FileAttachment]) : Seq[FileAttachment] = fileAttachments.filter { a =>
		val isIdentical = attachments.asScala.exists(f => f.name == a.name && f.isDataEqual(a))
		if (!isIdentical) {
			// if an attachment with the same name as this one exists then replace it
			val duplicateAttachment = attachments.asScala.find(_.name == a.name)
			duplicateAttachment.foreach(removeAttachment)
			addAttachment(a)
		}
		!isIdentical
	}

	def removeAttachment(attachment: FileAttachment): Boolean = {
		attachment.feedback = null
		attachment.markerFeedback = null
		attachments.remove(attachment)
	}

	def clearAttachments() {
		for(attachment <- attachments.asScala){
			attachment.feedback = null
		}
		attachments.clear()
	}
}

trait AssessmentFeedback {

	def hasGenericFeedback: Boolean

	def markingWorkflow: MarkingWorkflow

	/**
	 * Whether ratings are being collected for this feedback.
	 * Doesn't take into account whether the ratings feature is enabled, so you
	 * need to check that separately.
	 */
	def collectRatings: Boolean

	/**
	 * Whether marks are being collected for this feedback.
	 * Doesn't take into account whether the marks feature is enabled, so you
	 * need to check that separately.
	 */
	def collectMarks: Boolean

	def module: Module

	def academicYear: AcademicYear

	def assessmentGroups: JList[AssessmentGroup]
}

trait CM1WorkflowSupport {

	this: Feedback =>

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "first_marker_feedback")
	@Deprecated
	var firstMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "second_marker_feedback")
	@Deprecated
	var secondMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(PERSIST,MERGE,REFRESH,DETACH), fetch = FetchType.LAZY)
	@JoinColumn(name = "third_marker_feedback")
	@Deprecated
	var thirdMarkerFeedback: MarkerFeedback = _

	@Deprecated
	def getFeedbackPosition(markerFeedback: MarkerFeedback) : FeedbackPosition = {
		if(markerFeedback == firstMarkerFeedback) FirstFeedback
		else if (markerFeedback == secondMarkerFeedback) SecondFeedback
		else if (markerFeedback == thirdMarkerFeedback) ThirdFeedback
		else throw new IllegalArgumentException
	}

	// Returns None if marking is completed for the current workflow or if no workflow exists - i.e. not in the middle of a workflow
	@Deprecated
	def getCurrentWorkflowFeedbackPosition: Option[FeedbackPosition] = {

		def markingCompleted(workflow: MarkingWorkflow) = {
			(workflow.hasThirdMarker && thirdMarkerFeedback != null && thirdMarkerFeedback.state == MarkingState.MarkingCompleted) ||
				(!workflow.hasThirdMarker && workflow.hasSecondMarker && secondMarkerFeedback != null && secondMarkerFeedback.state == MarkingState.MarkingCompleted) ||
				(!workflow.hasThirdMarker && !workflow.hasSecondMarker && firstMarkerFeedback != null && firstMarkerFeedback.state == MarkingState.MarkingCompleted)
		}

		Option(markingWorkflow)
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

	@Deprecated
	def getCurrentWorkflowFeedback: Option[MarkerFeedback] = {
		getCurrentWorkflowFeedbackPosition match {
			case Some(FirstFeedback) => getFirstMarkerFeedback
			case Some(SecondFeedback) => getSecondMarkerFeedback
			case Some(ThirdFeedback) => getThirdMarkerFeedback
			case _ => None
		}
	}

	@Deprecated
	def getFirstMarkerFeedback: Option[MarkerFeedback] = Option(firstMarkerFeedback)
	@Deprecated
	def getSecondMarkerFeedback: Option[MarkerFeedback] = Option(secondMarkerFeedback)
	@Deprecated
	def getThirdMarkerFeedback: Option[MarkerFeedback] = Option(thirdMarkerFeedback)

	// The current workflow position isn't None so this must be a placeholder
	@Deprecated
	def isPlaceholder: Boolean = getCurrentWorkflowFeedbackPosition.isDefined || !hasContent

	@Deprecated
	def getAllMarkerFeedback: Seq[MarkerFeedback] = Seq(firstMarkerFeedback, secondMarkerFeedback, thirdMarkerFeedback)

	@Deprecated
	def getAllCompletedMarkerFeedback: Seq[MarkerFeedback] = Seq(firstMarkerFeedback, secondMarkerFeedback, thirdMarkerFeedback)
		.filter(_ != null)
		.filter(_.state == MarkingState.MarkingCompleted)
}

@Entity @Access(AccessType.FIELD)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
abstract class Feedback extends GeneratedId with FeedbackAttachments with PermissionsTarget with FormattedHtml with AssessmentFeedback with ToEntityReference with CM1WorkflowSupport{

	var uploaderId: String = _

	@Column(name = "uploaded_date")
	var createdDate: DateTime = new DateTime

	@Column(name = "updated_date")
	@NotNull
	var updatedDate: DateTime = new DateTime

	@Column(name = "universityId")
	var _universityId: String = _

	def universityId = Option(_universityId)

	def studentIdentifier = universityId.getOrElse(usercode)

	// TODO - ADD Not null constraint after bulk populating usercode @NotNull
	@Column(name = "userId")
	var usercode: String = _

	var released: JBoolean = false

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingPrompt: Option[Boolean] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingHelpful: Option[Boolean] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var actualMark: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var actualGrade: Option[String] = None

	@OneToMany(mappedBy = "feedback", cascade = Array(PERSIST,MERGE,REFRESH), fetch = FetchType.LAZY)
	@OrderBy("uploadedDate DESC")
	@BatchSize(size=200)
	private val _marks: JList[Mark] = JArrayList()
	def marks: _root_.uk.ac.warwick.tabula.JavaImports.JList[Mark] = _marks
	def addMark(uploaderId: String, markType: MarkType, mark: Int, grade: Option[String], reason: String, comments: String = null): Mark = {
		val newMark = new Mark
		newMark.feedback = this
		newMark.mark = mark
		newMark.grade = grade
		newMark.reason = reason
		newMark.comments = comments
		newMark.markType = markType
		newMark.uploaderId = uploaderId
		newMark.uploadedDate = DateTime.now
		_marks.add(0, newMark) // add at the top as we know it's the latest one, the rest get shifted down
		newMark
	}

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var agreedMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var agreedGrade: Option[String] = None

	def latestMark: Option[Int] = {
		if (agreedMark.isDefined)
			agreedMark
		else if (latestPrivateOrNonPrivateAdjustment.isDefined)
			latestPrivateOrNonPrivateAdjustment.map(_.mark)
		else
			actualMark
	}

	def latestGrade: Option[String] = {
		if (agreedGrade.isDefined)
			agreedGrade
		else if (latestPrivateOrNonPrivateAdjustment.isDefined)
			latestPrivateOrNonPrivateAdjustment.flatMap(_.grade)
		else
			actualGrade
	}

	def latestNonPrivateAdjustment: Option[Mark] = marks.asScala.find(_.markType == MarkType.Adjustment)
	def latestPrivateAdjustment: Option[Mark] = marks.asScala.find(_.markType == MarkType.PrivateAdjustment)
	def latestPrivateOrNonPrivateAdjustment: Option[Mark] = marks.asScala.headOption

	def adminViewableAdjustments: Seq[Mark] = marks.asScala

	// students can see the audit of non-private adjustments, back until the last private adjustment
	def studentViewableAdjustments: Seq[Mark] = {
		if (latestNonPrivateAdjustment.isDefined) {
			marks.asScala.takeWhile(mark => mark.markType != MarkType.PrivateAdjustment)
		} else Seq()
	}

	def hasPrivateAdjustments: Boolean = latestPrivateAdjustment.isDefined
	def hasNonPrivateAdjustments: Boolean = latestNonPrivateAdjustment.isDefined
	def hasPrivateOrNonPrivateAdjustments: Boolean = marks.asScala.nonEmpty

	def assessment: Assessment

	@OneToMany(mappedBy = "feedback", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var markerFeedback: JList[MarkerFeedback] = JArrayList()

	@ElementCollection @Column(name = "stage")
	@JoinTable(name = "OutstandingStages", joinColumns = Array(
		new JoinColumn(name = "feedback_id", referencedColumnName = "id")))
	@Type(`type` = "uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStageUserType")
	var outstandingStages: JList[MarkingWorkflowStage] = JArrayList()

	def isMarkingCompleted = outstandingStages.asScala.toList match {
		case (s: FinalStage) :: Nil => true
		case _ => false
	}

	@Column(name = "released_date")
	var releasedDate: DateTime = _

	@OneToMany(mappedBy = "feedback", cascade = Array(ALL))
	val customFormValues: JSet[SavedFormValue] = JHashSet()

	def clearCustomFormValues(): Unit = {
		customFormValues.asScala.foreach { v =>
			v.feedback = null
		}
		customFormValues.clear()
	}

	// FormValue containing the per-user online feedback comment
	def commentsFormValue: Option[SavedFormValue] = customFormValues.asScala.find(_.name == Assignment.defaultFeedbackTextFieldName)

	def comments: Option[String] = commentsFormValue.map(_.value)

	def commentsFormattedHtml: String = formattedHtml(comments)

	def hasContent: Boolean = hasMarkOrGrade || hasAttachments || hasOnlineFeedback

	def hasMarkOrGrade: Boolean = hasMark || hasGrade

	def hasMark: Boolean = actualMark.isDefined

	def hasGrade: Boolean = actualGrade.isDefined

	// TODO in some other places we also check that the string value hasText. Be consistent?
	def hasOnlineFeedback: Boolean = commentsFormValue.isDefined

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

}

@Entity @DiscriminatorValue("assignment")
class AssignmentFeedback extends Feedback {

	type Entity = AssignmentFeedback

	@ManyToOne(fetch = FetchType.LAZY, cascade=Array(PERSIST, MERGE))
	var assignment: Assignment = _

	def assessment: Assessment = assignment

	def module: Module = assignment.module

	override def markingWorkflow: MarkingWorkflow = assignment.markingWorkflow

	override def hasGenericFeedback: Boolean = Option(assignment.genericFeedback).isDefined

	override def collectMarks: Boolean = assignment.collectMarks

	override def collectRatings: Boolean = assignment.module.adminDepartment.collectFeedbackRatings

	override def academicYear: AcademicYear = assignment.academicYear

	override def assessmentGroups: JavaImports.JList[AssessmentGroup] = assignment.assessmentGroups

	def permissionsParents: Stream[Assignment] = Option(assignment).toStream

	override def toEntityReference: AssignmentFeedbackEntityReference = new AssignmentFeedbackEntityReference().put(this)

}

@Entity @DiscriminatorValue("exam")
class ExamFeedback extends Feedback {

	type Entity = ExamFeedback

	@ManyToOne(fetch = FetchType.LAZY, cascade=Array(PERSIST, MERGE))
	var exam: Exam = _

	def assessment: Assessment = exam

	def module: Module = exam.module

	override def markingWorkflow: MarkingWorkflow = null

	override def hasGenericFeedback: Boolean = false

	override def collectMarks: Boolean = true

	override def collectRatings: Boolean = false

	override def academicYear: AcademicYear = exam.academicYear

	override def assessmentGroups: JavaImports.JList[AssessmentGroup] = exam.assessmentGroups

	def permissionsParents: Stream[Exam] = Option(exam).toStream

	override def toEntityReference: ExamFeedbackEntityReference = new ExamFeedbackEntityReference().put(this)

}

object Feedback {
	val PublishDeadlineInWorkingDays = 20
}

@Deprecated
object FeedbackPosition {
	def getPreviousPosition(position: Option[FeedbackPosition]): Option[FeedbackPosition] = position match {
		case Some(FirstFeedback) => None
		case Some(SecondFeedback) => Option(FirstFeedback)
		case Some(ThirdFeedback) => Option(SecondFeedback)
		case None => Option(ThirdFeedback)
	}
}

@Deprecated
sealed trait FeedbackPosition extends Ordered[FeedbackPosition] {
	val description: String
	val position: Int
	def compare(that: FeedbackPosition): Int = this.position compare that.position
}
case object FirstFeedback extends FeedbackPosition { val description = "First marker's feedback"; val position = 1 }
case object SecondFeedback extends FeedbackPosition { val description = "Second marker's feedback"; val position = 2 }
case object ThirdFeedback extends FeedbackPosition { val description = "Third marker's feedback"; val position = 3 }

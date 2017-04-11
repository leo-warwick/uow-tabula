package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import org.joda.time.DateTime
import javax.persistence._

import org.hibernate.annotations.{BatchSize, Fetch, FetchMode, Type}
import javax.persistence.CascadeType._

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import javax.persistence.Entity

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.services.UserLookupService

@Entity @Access(AccessType.FIELD)
class MarkerFeedback extends GeneratedId with FeedbackAttachments with ToEntityReference with CanBeDeleted with CM1MarkerFeedbackSupport {
	type Entity = MarkerFeedback

	def this(parent:Feedback){
		this()
		feedback = parent
	}

	@transient
	var userLookup: UserLookupService = Wire[UserLookupService]("userLookup")

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "feedback_id", nullable = false)
	var feedback: Feedback = _

	@Column(name = "marker")
	private var markerUsercode: String = _

	def marker_=(marker: User) = markerUsercode = marker match {
		case m: User if !m.isFoundUser => throw new IllegalStateException(s"Marker is not a valid user.")
		case m: User =>  m.getUserId
		case _ => null
	}

	// returns an Anon user when the markerUsercode is null
	def marker: User =  {
		val marker = userLookup.getUserByUserId(markerUsercode)
		if (markerUsercode != null && !marker.isFoundUser)
			throw new IllegalStateException(s"Marker $markerUsercode is not a valid user")
		marker
	}

	def student: User = {
		val student = userLookup.getUserByUserId(feedback.usercode)
		if (!student.isFoundUser) throw new IllegalStateException(s"Student ${feedback.usercode} is not a valid user")
		student
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStageUserType")
	var stage: MarkingWorkflowStage = _

	@Column(name = "uploaded_date")
	var uploadedDate: DateTime = new DateTime

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var mark: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var grade: Option[String] = None

	@Deprecated
	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkingStateUserType")
	var state : MarkingState = _

	var rejectionComments: String = _

	@OneToMany(mappedBy = "markerFeedback", fetch = FetchType.LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	@Fetch(FetchMode.JOIN)
	var attachments: JList[FileAttachment] = JArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.markerFeedback = this
		attachments.add(attachment)
	}

	@OneToMany(mappedBy = "markerFeedback", cascade = Array(ALL))
	var customFormValues: JSet[SavedFormValue] = JHashSet()

	def getValue(field: FormField): Option[SavedFormValue] = {
		customFormValues.find( _.name == field.name )
	}

	def hasContent: Boolean = hasMarkOrGrade || hasFeedback || hasComments

	def hasMarkOrGrade: Boolean = hasMark || hasGrade

	def hasMark: Boolean = mark.isDefined

	def hasGrade: Boolean = grade.isDefined

	def hasFeedback: Boolean = attachments != null && attachments.size() > 0

	def hasComments: Boolean = customFormValues.exists(_.value != null)

	override def toEntityReference: MarkerFeedbackEntityReference = new MarkerFeedbackEntityReference().put(this)
}

trait CM1MarkerFeedbackSupport {
	this: MarkerFeedback =>

	@Deprecated
	def getMarkerUser: Option[User] = getMarkerUsercode.map(userLookup.getUserByUserId)

	@Deprecated
	def getFeedbackPosition: FeedbackPosition = feedback.getFeedbackPosition(this)

	@Deprecated
	def getMarkerUsercode: Option[String] = {
		// Very fuck you, Hibernate
		HibernateHelpers.initialiseAndUnproxy(feedback) match {
			case assignmentFeedback: AssignmentFeedback =>
				val student = feedback.usercode
				val assignment = assignmentFeedback.assignment
				Option(assignment.markingWorkflow).flatMap { workflow =>
					getFeedbackPosition match {
						case FirstFeedback => workflow.getStudentsFirstMarker(assignment, student)
						case SecondFeedback => workflow.getStudentsSecondMarker(assignment, student)
						case ThirdFeedback => workflow.getStudentsFirstMarker(assignment, student)
						case _ => None
					}
				}
			case _ => None
		}
	}
}

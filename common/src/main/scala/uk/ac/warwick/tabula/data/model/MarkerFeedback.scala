package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.{Entity, _}
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, ModerationStage}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class MarkerFeedback extends GeneratedId
  with FeedbackAttachments
  with ToEntityReference
  with CanBeDeleted {
  type Entity = MarkerFeedback

  def this(parent: Feedback) = {
    this()
    feedback = parent
  }

  @transient
  var userLookup: UserLookupService = Wire[UserLookupService]("userLookup")

  @transient
  var profileService: ProfileService = Wire[ProfileService]

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "feedback_id", nullable = false)
  var feedback: Feedback = _

  @Column(name = "marker")
  private var markerUsercode: String = _

  def marker_=(marker: User): Unit = markerUsercode = marker match {
    case m: User if !m.isFoundUser => throw new IllegalStateException(s"${m.getUserId} is not a valid user.")
    case m: User => m.getUserId
    case _ => null
  }

  // returns an Anon user when the markerUsercode is null
  def marker: User = {
    val marker = userLookup.getUserByUserId(markerUsercode)
    if (markerUsercode.hasText && !marker.isFoundUser)
      dummyUser(ofUserType = MemberUserType.Other, userId = markerUsercode)
    else
      marker
  }

  // get user profile from tabula if not found from sso
  // or create a dummy user with the same usercode
  @transient
  def dummyUser(ofUserType: MemberUserType, userId: String): User = {
    val possibleMembers = profileService.getAllMembersWithUserId(
      userId = userId,
      disableFilter = true,
      activeOnly = false
    ).distinct

    if (possibleMembers.isEmpty) {
      val dummyUser = new User
      dummyUser.setUserId(userId)
      dummyUser.setWarwickId(feedback.universityId.getOrElse("Unknown University ID"))
      dummyUser.setFirstName("[Unknown user]")
      dummyUser.setLastName("[Unknown user]")
      dummyUser.setFullName("[Unknown user]")
      dummyUser.setLoginDisabled(true)
      dummyUser.setUserType(ofUserType.description)
      dummyUser.setExtraProperties(JHashMap(
        "urn:websignon:usertype" -> dummyUser.getUserType,
        "urn:websignon:timestamp" -> DateTime.now.toString,
        "urn:websignon:usersource" -> "Tabula"
      ))
      dummyUser.setVerified(false)
      dummyUser.setFoundUser(true)
      dummyUser
    } else {
      possibleMembers.head.asSsoUser
    }
  }

  def student: User = {
    val studentUsercode: String = feedback.usercode
    val student = userLookup.getUserByUserId(studentUsercode)
    if (!student.isFoundUser)
      dummyUser(MemberUserType.Student, studentUsercode)
    else
      student
  }

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStageUserType")
  var stage: MarkingWorkflowStage = _

  @Column(name = "uploaded_date")
  var uploadedDate: DateTime = new DateTime

  // initially starts as null - if the marker makes any changes then this is set
  // allows us to distinguish between feedback that has been approved (copied from the previous stage) and feedback that has been modified
  @Column(name = "updated_on")
  var updatedOn: DateTime = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var mark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var grade: Option[String] = None

  @OneToMany(mappedBy = "markerFeedback", fetch = FetchType.EAGER, cascade = Array(ALL))
  @BatchSize(size = 200)
  var attachments: JSet[FileAttachment] = JHashSet()

  def addAttachment(attachment: FileAttachment): Unit = {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.markerFeedback = this
    attachments.add(attachment)
  }

  @OneToMany(mappedBy = "markerFeedback", cascade = Array(ALL))
  var customFormValues: JSet[SavedFormValue] = JHashSet()

  def clearCustomFormValues(): Unit = {
    customFormValues.asScala.foreach { v =>
      v.markerFeedback = null
    }
    customFormValues.clear()
  }

  def getValue(field: FormField): Option[SavedFormValue] = {
    customFormValues.asScala.find(_.name == field.name)
  }

  def comments: Option[String] = fieldValue(Assignment.defaultFeedbackTextFieldName)
  def comments_=(value: String): Unit = setFieldValue(Assignment.defaultFeedbackTextFieldName, value)

  def fieldValue(fieldName: String): Option[String] = customFormValues.asScala.find(_.name == fieldName).map(_.value)

  def setFieldValue(fieldName: String, value: String): Unit = {
    customFormValues.asScala
      .find(_.name == fieldName)
      .getOrElse {
        val newValue = new SavedFormValue
        newValue.name = fieldName
        newValue.markerFeedback = this
        customFormValues.add(newValue)
        newValue
      }.value = value
  }

  def fieldNameValuePairsMap: Map[String, String] = customFormValues.asScala.flatMap { formValue =>
    feedback.assignment.feedbackFields.find(_.name == formValue.name).map { feedbackField =>
      feedbackField.name -> formValue.value
    }
  }.toMap


  def hasBeenModified: Boolean = updatedOn != null

  def moderationSkipped: Boolean = stage.isInstanceOf[ModerationStage] && !feedback.wasModerated

  def hasContent: Boolean = hasMarkOrGrade || hasFeedbackOrComments

  def hasMarkOrGrade: Boolean = hasMark || hasGrade

  def hasMark: Boolean = mark.isDefined

  def hasGrade: Boolean = grade.isDefined

  def hasFeedbackOrComments: Boolean = hasFeedback || hasComments

  def hasFeedback: Boolean = attachments != null && attachments.size() > 0

  def hasComments: Boolean = customFormValues.asScala.exists(_.value != null)

  def readyForNextStage: Boolean = hasContent && outstanding

  def outstanding: Boolean = feedback.outstandingStages.contains(stage)

  def finalised: Boolean = hasContent && feedback.currentStageIndex > stage.order
}

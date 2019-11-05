package uk.ac.warwick.tabula.data.model.forms

import java.sql.Types

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.`type`.{StandardBasicTypes, StringType}
import org.hibernate.annotations.{BatchSize, Formula, Proxy, Type}
import org.joda.time.{DateTime, Days}
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.ExtensionState.Approved
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.jdk.CollectionConverters._
import scala.language.implicitConversions

@Entity
@Proxy
@Access(AccessType.FIELD)
class Extension extends GeneratedId with PermissionsTarget with ToEntityReference {

  type Entity = Extension

  @ManyToOne(optional = false, cascade = Array(PERSIST, MERGE), fetch = FetchType.LAZY)
  @JoinColumn(name = "assignment_id")
  var assignment: Assignment = _

  @transient
  var profileService: ProfileService = Wire[ProfileService]

  def permissionsParents: Stream[PermissionsTarget] =
    Option(assignment).toStream #::: profileService.getAllMembersWithUserId(usercode).toStream

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "universityid", insertable = false, updatable = false)
  var member: Member = _

  @NotNull
  @Column(name = "userId")
  var usercode: String = _

  @Column(name = "universityId")
  var _universityId: String = _

  @Formula("(select coalesce(expiryDate, assignment.closeDate) from assignment where assignment.id = assignment_id and assignment.openEnded = false)")
  var expiryDateOrAssignmentCloseDate: DateTime = _

  @Formula("(select date_part('day', coalesce(expiryDate, requestedExpiryDate, assignment.closeDate) - assignment.closeDate) from assignment where assignment.id = assignment_id and assignment.openEnded = false)")
  var lengthDays: JInteger = _

  override def humanReadableId: String = s"Extension for $usercode for ${assignment.humanReadableId}"

  def universityId = Option(_universityId)

  def studentIdentifier: String = universityId.getOrElse(usercode)

  def isForUser(user: User): Boolean = isForUser(user.getUserId)

  def isForUser(theUsercode: String): Boolean = usercode == theUsercode

  // TODO should there be a single def that returns the expiry date for approved/manual extensions, and requested expiry date otherwise?
  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  @Column(name = "requestedExpiryDate")
  private var _requestedExpiryDate: DateTime = _

  def requestedExpiryDate: Option[DateTime] = {
    if (assignment.openEnded) {
      None
    } else {
      Option(_requestedExpiryDate)
    }
  }

  def requestedExpiryDate_=(red: DateTime) {
    _requestedExpiryDate = red
  }

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  @Column(name = "expiryDate")
  private var _expiryDate: DateTime = _

  def expiryDate_=(ed: DateTime) {
    _expiryDate = ed
  }

  def expiryDate: Option[DateTime] = {
    if (_expiryDate == null || state != Approved || assignment.openEnded || assignment.closeDate == null) None
    else Option(_expiryDate)
  }

  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  var requestedOn: DateTime = _
  @Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  @Column(name = "approvedOn")
  var reviewedOn: DateTime = _

  var reason: String = _
  @Column(name = "approvalComments")
  var reviewerComments: String = _
  var disabilityAdjustment: Boolean = false

  @Column(name = "state")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.forms.ExtensionStateUserType")
  var _state: ExtensionState = ExtensionState.Unreviewed

  def state: ExtensionState = _state

  /** Don't use rawState_ directly, call approve() or reject() instead **/
  def rawState_=(state: ExtensionState) {
    _state = state
  }


  @OneToMany(mappedBy = "extension", fetch = LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var attachments: JSet[FileAttachment] = JSet()

  def nonEmptyAttachments: Seq[FileAttachment] = attachments.asScala.toSeq.filter(_.hasData)

  def addAttachment(attachment: FileAttachment) {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.extension = this
    attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.extension = null
    attachments.remove(attachment)
  }

  // this extension was manually created by an administrator, rather than requested by a student
  def isManual: Boolean = requestedOn == null

  def isInitiatedByStudent: Boolean = !isManual

  // you can't infer from state alone whether there's a request outstanding - use awaitingReview()
  def approved: Boolean = state == ExtensionState.Approved

  def rejected: Boolean = state == ExtensionState.Rejected

  def unreviewed: Boolean = state == ExtensionState.Unreviewed

  def revoked: Boolean = state == ExtensionState.Revoked

  def moreInfoRequired: Boolean = state == ExtensionState.MoreInformationRequired

  def moreInfoReceived: Boolean = state == ExtensionState.MoreInformationReceived

  def rejectable: Boolean = awaitingReview || (approved && isInitiatedByStudent)

  def revocable: Boolean = approved && !isInitiatedByStudent

  def updateState(newState: ExtensionState, comments: String): Unit = {
    _state = newState
    reviewedOn = DateTime.now
    reviewerComments = comments
  }

  // keep state encapsulated
  def approve(comments: String = null): Unit = updateState(ExtensionState.Approved, comments)

  def reject(comments: String = null): Unit = updateState(ExtensionState.Rejected, comments)

  def revoke(comments: String = null): Unit = updateState(ExtensionState.Revoked, comments)

  def requestMoreInfo(comments: String = null): Unit = updateState(ExtensionState.MoreInformationRequired, comments)

  def awaitingReview: Boolean = {
    // wrap nullable dates to be more readable in pattern match
    val requestDate = Option(requestedOn)
    val reviewDate = Option(reviewedOn)

    (requestDate, reviewDate) match {
      case (Some(_), None) => true
      case (Some(latestRequest), Some(lastReview)) if latestRequest.isAfter(lastReview) => true
      case _ => false
    }
  }

  @transient
  lazy val workingDaysHelper = new WorkingDaysHelperImpl

  // calculate deadline only if not late (return None for late returns)
  def feedbackDeadline: Option[DateTime] = assignment
    .findSubmission(usercode)
    .flatMap(s => expiryDate.map(_.isAfter(s.submittedDate)))
    .collect {
      case true => feedbackDueDate
    }.flatten

  // the feedback deadline if an expry date exists for this extension
  def feedbackDueDate: Option[DateTime] = expiryDate.map(ed =>
    workingDaysHelper.datePlusWorkingDays(ed.toLocalDate.asJava, Feedback.PublishDeadlineInWorkingDays).asJoda.toDateTime(ed)
  )

  def duration: Int = expiryDate.map(Days.daysBetween(assignment.closeDate, _).getDays).getOrElse(0)

  def requestedExtraDuration: Int =
    requestedExpiryDate.orElse(expiryDate)
      .map(Days.daysBetween(assignment.closeDate, _).getDays).getOrElse(0)

  // An extension is relevant if the expiry date is after the assignment close date.
  // Extensions can become irrelevant if the assignment close date is moved back
  // after an extension has been approved.
  def relevant: Boolean = expiryDate.exists(_.isAfter(assignment.closeDate))

  // Whether the approved expiry date is different from the requested expiry date
  def expiryDateAdjusted: Boolean = expiryDate.exists(date => !requestedExpiryDate.contains(date))
}


object Extension {
  val MaxDaysToDisplayAsProgressBar: Int = 8 * 7
}


sealed abstract class ExtensionState(val dbValue: String, val description: String)

object ExtensionState {

  // you can't infer from state alone whether there's a request outstanding - use extension.awaitingReview()
  case object Unreviewed extends ExtensionState("U", "Unreviewed")

  case object Approved extends ExtensionState("A", "Approved")

  case object MoreInformationRequired extends ExtensionState("M", "More info required")

  case object MoreInformationReceived extends ExtensionState("C", "More info received")

  case object Rejected extends ExtensionState("R", "Rejected")

  case object Revoked extends ExtensionState("V", "Revoked")

  def fromCode(code: String): ExtensionState = code match {
    case Unreviewed.dbValue => Unreviewed
    case Approved.dbValue => Approved
    case MoreInformationRequired.dbValue => MoreInformationRequired
    case MoreInformationReceived.dbValue => MoreInformationReceived
    case Rejected.dbValue => Rejected
    case Revoked.dbValue => Revoked
    case _ => throw new IllegalArgumentException()
  }

  def all: Seq[ExtensionState] = Seq(Unreviewed, Approved, Rejected, Revoked, MoreInformationRequired, MoreInformationReceived)
}

class ExtensionStateUserType extends AbstractBasicUserType[ExtensionState, String] {
  val basicType: StringType = StandardBasicTypes.STRING

  override def sqlTypes = Array(Types.VARCHAR)

  override val nullValue: String = null
  override val nullObject: ExtensionState = null

  override def convertToObject(string: String): ExtensionState = ExtensionState.fromCode(string)

  override def convertToValue(es: ExtensionState): String = es.dbValue
}

class ExtensionStateConverter extends TwoWayConverter[String, ExtensionState] {
  override def convertRight(code: String): ExtensionState = ExtensionState.fromCode(code)

  override def convertLeft(state: ExtensionState): String = Option(state).map(_.dbValue).orNull
}

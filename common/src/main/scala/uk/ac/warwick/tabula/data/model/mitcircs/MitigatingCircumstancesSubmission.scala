package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesSubmission extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable
  with ToEntityReference {
    type Entity = MitigatingCircumstancesSubmission

  def this(student: StudentMember, creator: User, department: Department) = {
    this()
    this.creator = creator
    this.lastModifiedBy = creator
    this.student = student
    this.department = department
  }

  @Column(nullable = false, unique = true)
  var key: JLong = _

  @Column(nullable = false)
  val createdDate: DateTime = DateTime.now()

  @Column(name = "lastModified", nullable = false)
  private var _lastModified: DateTime = DateTime.now()

  /**
    * This will return the latest of:
    * - the submission's last modified date
    * - the latest message that was sent
    * - the latest modified date of any notes
    */
  def lastModified: DateTime = transactional(readOnly = true) {
    Seq(
      Option(_lastModified),
      messages.sortBy(_.createdDate).lastOption.map(_.createdDate),
      notes.sortBy(_.lastModified).lastOption.map(_.lastModified),
    ).flatten.max
  }

  def lastModified_=(lastModified: DateTime): Unit = _lastModified = lastModified

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var creator: User = _ // the user that created this

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var lastModifiedBy: User = _

  @Column(nullable = true)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var outcomesLastRecordedBy: User = _

  @Column(nullable = true)
  var outcomesLastRecordedOn: DateTime = _

  @Column(nullable = true)
  var outcomesSubmittedOn: DateTime = _

  @Column(nullable = true)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var outcomesApprovedBy: User = _

  @Column(nullable = true)
  var outcomesApprovedOn: DateTime = _

  def outcomesFinalisedOn: DateTime = if(isAcute) outcomesLastRecordedOn else outcomesApprovedOn

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "universityId", referencedColumnName = "universityId")
  var student: StudentMember = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "relatedSubmission")
  var relatedSubmission: MitigatingCircumstancesSubmission = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "panel_id")
  private var _panel: MitigatingCircumstancesPanel = _
  def panel: Option[MitigatingCircumstancesPanel] = Option(_panel)
  def panel_=(panel: MitigatingCircumstancesPanel): Unit = _panel = panel

  @Column(nullable = true) // Nullable for drafts
  var startDate: LocalDate = _

  @Column(nullable = true)
  var endDate: LocalDate = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.IssueTypeUserType")
  var issueTypes: Seq[IssueType] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "issueTypeDetails")
  private var encryptedIssueTypeDetails: CharSequence = _

  // free text for use when the issue type includes Other
  def issueTypeDetails: String = Option(encryptedIssueTypeDetails).map(_.toString).orNull
  def issueTypeDetails_=(issueTypeDetails: String): Unit = encryptedIssueTypeDetails = issueTypeDetails

  var contacted: JBoolean = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitCircsContactUserType")
  var contacts: Seq[MitCircsContact] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "contactOther")
  private var encryptedContactOther: CharSequence = _

  // free text for use when the contacts includes Other
  def contactOther: String = Option(encryptedContactOther).map(_.toString).orNull
  def contactOther_=(contactOther: String): Unit = encryptedContactOther = contactOther

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "noContactReason")
  private var encryptedNoContactReason: CharSequence = _
  def noContactReason: String = Option(encryptedNoContactReason).map(_.toString).orNull
  def noContactReason_=(noContactReason: String): Unit = encryptedNoContactReason = noContactReason

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "reason", nullable = false)
  private var encryptedReason: CharSequence = _
  def reason: String = Option(encryptedReason).map(_.toString).orNull
  def reason_=(reason: String): Unit = encryptedReason = reason

  def formattedReason: TemplateHTMLOutputModel = FormattedHtml(reason)

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(ALL))
  @JoinColumn(name = "submission_id")
  @BatchSize(size = 200)
  @OrderBy("academicYear, moduleCode, sequence")
  var affectedAssessments: JList[MitigatingCircumstancesAffectedAssessment] = JArrayList()

  def affectedAssessmentsByRecommendation: Map[AssessmentSpecificRecommendation, Seq[MitigatingCircumstancesAffectedAssessment]] =
    MitCircsExamBoardRecommendation.values.collect{ case r: AssessmentSpecificRecommendation => r }
      .map{ r => r -> affectedAssessments.asScala.toSeq.filter{ a => Option(a.boardRecommendations).exists(_.contains(r)) } }
      .filter{ case (_, a) => a.nonEmpty }
      .toMap

  def globalRecommendations: Seq[MitCircsExamBoardRecommendation] = Option(boardRecommendations).map(_.diff(
    boardRecommendations.collect{ case r: AssessmentSpecificRecommendation => r }
  )).getOrElse(Nil)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "pendingEvidence", nullable = false)
  private var encryptedPendingEvidence: CharSequence = _
  def pendingEvidence: String = Option(encryptedPendingEvidence).map(_.toString).orNull
  def pendingEvidence_=(pendingEvidence: String): Unit = encryptedPendingEvidence = pendingEvidence

  def formattedPendingEvidence: TemplateHTMLOutputModel = FormattedHtml(pendingEvidence)

  var pendingEvidenceDue: LocalDate = _

  @Column(nullable = false)
  var hasSensitiveEvidence: Boolean = false

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var sensitiveEvidenceSeenBy: User = _

  var sensitiveEvidenceSeenOn: LocalDate = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "sensitiveEvidenceComments")
  private var encryptedSensitiveEvidenceComments: CharSequence = _
  def sensitiveEvidenceComments: String = Option(encryptedSensitiveEvidenceComments).map(_.toString).orNull
  def sensitiveEvidenceComments_=(sensitiveEvidenceComments: String): Unit = encryptedSensitiveEvidenceComments = sensitiveEvidenceComments
  def formattedSensitiveEvidenceComments: TemplateHTMLOutputModel = FormattedHtml(sensitiveEvidenceComments)

  // Date the student approved (finally submits) the submission - (may have been raised on their behalf)
  @Column(name = "approvedOn")
  private var _approvedOn: DateTime = _

  // Guarded against being set directly; use .approveAndSubmit() below
  def approvedOn: Option[DateTime] = Option(_approvedOn)

  @Column(nullable = false)
  var covid19Submission: Boolean = false

  @OneToMany(mappedBy = "mitigatingCircumstancesSubmission", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  private val _attachments: JSet[FileAttachment] = JHashSet()
  def attachments: Seq[FileAttachment] = transactional(readOnly = true) {
    // files attached to messages sent before outcomes were recorded are treated as evidence
    val messageEvidence = messages.filter(m => m.studentSent && Option(outcomesSubmittedOn).forall(m.createdDate.isBefore)).flatMap(_.attachments)
    (_attachments.asScala.toSeq ++ messageEvidence).sortBy(_.dateUploaded)
  }

  def addAttachment(attachment: FileAttachment): Unit = {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.mitigatingCircumstancesSubmission = this
    _attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.mitigatingCircumstancesSubmission = null
    _attachments.remove(attachment)
  }

  @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY)
  @BatchSize(size = 200)
  private val _messages: JSet[MitigatingCircumstancesMessage] = JHashSet()
  def messages: Seq[MitigatingCircumstancesMessage] = _messages.asScala.toSeq.sortBy(_.createdDate)

  @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY)
  @BatchSize(size = 200)
  private val _notes: JSet[MitigatingCircumstancesNote] = JHashSet()
  def notes: Seq[MitigatingCircumstancesNote] = _notes.asScala.toSeq.sortBy(_.createdDate)

  @Column(name = "lastViewedByStudent")
  private var _lastViewedByStudent: DateTime = _
  def lastViewedByStudent: Option[DateTime] = Option(_lastViewedByStudent)
  def lastViewedByStudent_=(dt: DateTime): Unit = _lastViewedByStudent = dt

  @Column(name = "lastViewedByOfficer")
  private var _lastViewedByOfficer: DateTime = _
  def lastViewedByOfficer: Option[DateTime] = Option(_lastViewedByOfficer)
  def lastViewedByOfficer_=(dt: DateTime): Unit = _lastViewedByOfficer = dt

  // Doesn't include notes, whereas normally lastModified does, and filters messages
  private def lastModifiedIncludingMessages(messageFilter: MitigatingCircumstancesMessage => Boolean): DateTime = {
    Seq(
      Option(_lastModified),
      messages.filter(messageFilter).sortBy(_.createdDate).lastOption.map(_.createdDate)
    ).flatten.max
  }

  def isUnreadByOfficer: Boolean = transactional(readOnly = true) {
    val lastModified = lastModifiedIncludingMessages(_.studentSent) // only includes student-sent messages
    lastViewedByOfficer.forall(_.isBefore(lastModified))
  }

  def isUnreadByStudent: Boolean = transactional(readOnly = true) {
    val lastModified = lastModifiedIncludingMessages(m => !m.studentSent) // only includes staff-sent messages
    lastViewedByStudent.forall(_.isBefore(lastModified))
  }

  // Outcomes

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesGradingUserType")
  var outcomeGrading: MitigatingCircumstancesGrading = _

  // '0' is appended to the grading code for acute mitigation to ensure that mitigation is not "double counted" in exam boards
  def gradingCode: Option[String] = {
    Option(outcomeGrading).map(o => if(isAcute) s"${o.code}0" else o.code)
  }

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "outcomeReasons")
  private var encryptedOutcomeReasons: CharSequence = _
  def outcomeReasons: String = Option(encryptedOutcomeReasons).map(_.toString).orNull
  def outcomeReasons_=(outcomeReasons: String): Unit = encryptedOutcomeReasons = outcomeReasons

  def formattedOutcomeReasons: TemplateHTMLOutputModel = FormattedHtml(outcomeReasons)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitCircsExamBoardRecommendationUserType")
  var boardRecommendations: Seq[MitCircsExamBoardRecommendation] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "boardRecommendationOther")
  private var encryptedBoardRecommendationOther: CharSequence = _
  // free text for use when the boardRecommendations type includes Other
  def boardRecommendationOther: String = Option(encryptedBoardRecommendationOther).map(_.toString).orNull
  def boardRecommendationOther_=(boardRecommendationOther: String): Unit = encryptedBoardRecommendationOther = boardRecommendationOther

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "boardRecommendationComments")
  private var encryptedBoardRecommendationComments: CharSequence = _
  def boardRecommendationComments: String = Option(encryptedBoardRecommendationComments).map(_.toString).orNull
  def boardRecommendationComments_=(boardRecommendationComments: String): Unit = encryptedBoardRecommendationComments = boardRecommendationComments

  def formattedBoardRecommendationComments: TemplateHTMLOutputModel = FormattedHtml(boardRecommendationComments)

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesRejectionReasonUserType")
  var rejectionReasons: Seq[MitigatingCircumstancesRejectionReason] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(name = "rejectionReasonsOther")
  private var encryptedRejectionReasonsOther: CharSequence = _
  // free text for use when the rejectionReasons type includes Other
  def rejectionReasonsOther: String = Option(encryptedRejectionReasonsOther).map(_.toString).orNull
  def rejectionReasonsOther_=(rejectionReasonOther: String): Unit = encryptedRejectionReasonsOther = rejectionReasonOther

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesAcuteOutcomeUserType")
  var acuteOutcome: MitigatingCircumstancesAcuteOutcome = _

  // get's the list of assessments which the actue outcome apples to (may be a subset of the assessments that the student indicated were affected)
  def assessmentsWithAcuteOutcome: Seq[MitigatingCircumstancesAffectedAssessment] = affectedAssessments.asScala.toSeq
    .filter(a => a.acuteOutcome != null && a.acuteOutcome == acuteOutcome)

  // Intentionally no default here, rely on a state being set explicitly
  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionStateUserType")
  @Column(name = "state", nullable = false)
  private var _state: MitigatingCircumstancesSubmissionState = _

  // Can get but not set state directly, use the methods below to transition
  def state: MitigatingCircumstancesSubmissionState = _state

  // State transitions
  def saveAsDraft(): Unit = {
    require(Option(_state).isEmpty || Seq(Draft, CreatedOnBehalfOfStudent, Submitted).contains(_state), "Cannot return a submission to draft if it has already been actioned")
    _state = Draft
    _approvedOn = null
  }

  def saveOnBehalfOfStudent(): Unit = {
    require(Option(_state).isEmpty || Seq(Draft, CreatedOnBehalfOfStudent).contains(_state), "Cannot save on behalf of a student if they have already submitted")
    _state = CreatedOnBehalfOfStudent
    _approvedOn = null
  }

  def approveAndSubmit(): Unit = {
    require(Option(_state).isEmpty || Seq(Draft, CreatedOnBehalfOfStudent, Submitted).contains(_state), "Cannot approve and submit if it has already been actioned")
    _state = Submitted
    _approvedOn = DateTime.now()
  }

  def readyForPanel(): Unit = {
    require(_state == Submitted, "Cannot set as ready for review until this has been submitted by the student")
    _state = ReadyForPanel
  }

  def notReadyForPanel(): Unit = {
    require(_state == ReadyForPanel, "Cannot set as not ready for panel")
    _state = Submitted
  }

  def outcomesRecorded(user: Option[User] = None): Unit = {
    require(Seq(Submitted, ReadyForPanel, OutcomesRecorded, ApprovedByChair).contains(_state), "Cannot record outcomes until this has been submitted by the student")

    // Only trigger this the first time that outcomes are submitted
    if (_state != OutcomesRecorded) {
      outcomesSubmittedOn = DateTime.now
    }

    _state = OutcomesRecorded

    user.foreach { u =>
      if (panel.exists(_.chair.contains(u))) {
        approvedByChair(u)
      }
    }
  }

  def approvedByChair(user: User): Unit = {
    require(_state == OutcomesRecorded)
    outcomesApprovedOn = DateTime.now
    outcomesApprovedBy = user
    _state = ApprovedByChair
  }

  def unApprovedByChair(): Unit = {
    require(_state == ApprovedByChair)
    outcomesApprovedOn = null
    outcomesApprovedBy = null
    _state = OutcomesRecorded
  }


  def withdraw(): Unit = {
    // A student can withdraw a submission at any time UNLESS it's had outcomes recorded
    require(canWithdraw, "Cannot withdraw a submission that has outcomes recorded")
    _state = Withdrawn
    panel = null
  }

  def reopen(): Unit = {
    require(canReopen, "Cannot re-open a submission that hasn't been withdrawn")
    _state = Draft
  }

  def isDraft: Boolean = Seq(Draft, CreatedOnBehalfOfStudent).contains(state)
  def isEditable(user: User): Boolean = {
    state == MitigatingCircumstancesSubmissionState.CreatedOnBehalfOfStudent ||
    (user.getWarwickId == student.universityId && (
      state == MitigatingCircumstancesSubmissionState.Draft ||
      state == MitigatingCircumstancesSubmissionState.Submitted
    ))
  }

  def isWithdrawn: Boolean = state == MitigatingCircumstancesSubmissionState.Withdrawn

  def hasEvidence: Boolean = attachments.nonEmpty
  def isEvidencePending: Boolean = !isWithdrawn && !Seq(OutcomesRecorded, ApprovedByChair).contains(state) && pendingEvidenceDue != null
  def isAcute: Boolean = Option(acuteOutcome).isDefined
  def canConfirmSensitiveEvidence: Boolean = !isWithdrawn && !Seq(OutcomesRecorded, ApprovedByChair).contains(state)
  def canRecordOutcomes: Boolean = !isWithdrawn && !isDraft && (state == ReadyForPanel || state == OutcomesRecorded || state == ApprovedByChair || panel.nonEmpty) && (!Seq(OutcomesRecorded, ApprovedByChair).contains(state) || !isAcute)
  def canRecordAcuteOutcomes: Boolean = !isWithdrawn && !isDraft && state != ReadyForPanel && ((panel.isEmpty && !Seq(OutcomesRecorded, ApprovedByChair).contains(state)) || isAcute)
  def canWithdraw: Boolean = !Seq(OutcomesRecorded, ApprovedByChair).contains(state)
  def canApproveOutcomes: Boolean = !isWithdrawn && !isDraft && state == OutcomesRecorded && !isAcute
  def canReopen: Boolean = isWithdrawn
  def canAddNote: Boolean = !isWithdrawn && !isDraft
  def canAddMessage: Boolean =
    !isWithdrawn &&
    state != Draft && // Allow CreatedOnBehalfOf
    Option(outcomesSubmittedOn).forall(_.plusMonths(1).isAfterNow) // TAB-7330 Don't allow messages one month after outcomes

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "student" -> student.universityId,
    "creator" -> creator.getWarwickId
  )

  // Don't use the student directly as the permission parent here. We don't want permissions to bubble up to all the students touchedDepartments
  override def permissionsParents: LazyList[PermissionsTarget] = panel.to(LazyList) #::: LazyList(MitigatingCircumstancesStudent(student))
}





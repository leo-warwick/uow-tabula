package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
 * A set of marks for a student on a particular AssessmentComponent, as recorded in Tabula. This is
 * *NOT* canonical, the canonical mark should use the fields on UpstreamAssessmentGroupMember, but it
 * is guaranteed to be persistent - it will not be removed if the marks record is removed from SITS nor
 * will it be updated by updates in SITS; it is a permanent record of marks that have been recorded in
 * Tabula for upload into SITS.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedAssessmentComponentStudent extends GeneratedId
  with HibernateVersioned
  with ToString {

  def this(uagm: UpstreamAssessmentGroupMember) = {
    this()
    this.moduleCode = uagm.upstreamAssessmentGroup.moduleCode
    this.assessmentGroup = uagm.upstreamAssessmentGroup.assessmentGroup
    this.occurrence = uagm.upstreamAssessmentGroup.occurrence
    this.sequence = uagm.upstreamAssessmentGroup.sequence
    this.academicYear = uagm.upstreamAssessmentGroup.academicYear
    this.universityId = uagm.universityId
    this.assessmentType = uagm.assessmentType
    this.resitSequence = uagm.resitSequence
  }

  // Properties for linking through to AssessmentComponent/UpstreamAssessmentGroup/UpstreamAssessmentGroupMember

  // Long-form module code with hyphen and CATS value
  @Column(name = "module_code", nullable = false)
  var moduleCode: String = _

  @Column(name = "assessment_group", nullable = false)
  var assessmentGroup: String = _

  @Column(nullable = false)
  var occurrence: String = _

  @Column(nullable = false)
  var sequence: String = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "university_id", nullable = false)
  var universityId: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroupMemberAssessmentTypeUserType")
  @Column(name = "assessment_type", nullable = false)
  var assessmentType: UpstreamAssessmentGroupMemberAssessmentType = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  @Column(name = "resit_sequence")
  var resitSequence: Option[String] = None

  /**
   * Doesn't check that this is for the same group, just that it's for the same person on the group
   */
  def matchesIdentity(uagm: UpstreamAssessmentGroupMember): Boolean =
    universityId == uagm.universityId && assessmentType == uagm.assessmentType && resitSequence == uagm.resitSequence

  @OneToMany(mappedBy = "recordedAssessmentComponentStudent", cascade = Array(ALL), fetch = FetchType.EAGER)
  @OrderBy("updated_date DESC")
  @BatchSize(size = 200)
  private val _marks: JList[RecordedAssessmentComponentStudentMark] = JArrayList()
  def marks: Seq[RecordedAssessmentComponentStudentMark] = _marks.asScala.toSeq

  def addMark(
    uploader: User,
    mark: Option[Int],
    grade: Option[String],
    source: RecordedAssessmentComponentStudentMarkSource,
    markState: MarkState,
    comments: String = null
  ): RecordedAssessmentComponentStudentMark = {
    val newMark = new RecordedAssessmentComponentStudentMark
    newMark.recordedAssessmentComponentStudent = this
    newMark.mark = mark
    newMark.grade = grade
    newMark.comments = comments
    newMark.source = source
    newMark.markState = markState
    newMark.updatedBy = uploader
    newMark.updatedDate = DateTime.now
    _marks.add(0, newMark) // add at the top as we know it's the latest one, the rest get shifted down
    needsWritingToSitsSince = Some(DateTime.now)
    newMark
  }

  def latestMark: Option[Int] = marks.headOption.flatMap(_.mark)
  def latestGrade: Option[String] = marks.headOption.flatMap(_.grade)
  def latestState: Option[MarkState] = marks.headOption.map(_.markState)

  @Column(name = "needs_writing_to_sits_since")
  private var _needsWritingToSitsSince: DateTime = _
  def needsWritingToSitsSince: Option[DateTime] = Option(_needsWritingToSitsSince)
  def needsWritingToSitsSince_=(needsWritingToSitsSince: Option[DateTime]): Unit = _needsWritingToSitsSince = needsWritingToSitsSince.orNull
  def needsWritingToSits: Boolean = needsWritingToSitsSince.nonEmpty

  // empty for any student that's never been written
  @Column(name = "last_written_to_sits")
  private var _lastWrittenToSits: DateTime = _
  def lastWrittenToSits: Option[DateTime] = Option(_lastWrittenToSits)
  def lastWrittenToSits_=(lastWrittenToSits: Option[DateTime]): Unit = _lastWrittenToSits = lastWrittenToSits.orNull

  @Column(name = "last_sits_write_error_date")
  private var _lastSitsWriteErrorDate: DateTime = _
  def lastSitsWriteErrorDate: Option[DateTime] = Option(_lastSitsWriteErrorDate)
  def lastSitsWriteErrorDate_=(lastSitsWriteErrorDate: Option[DateTime]): Unit = _lastSitsWriteErrorDate = lastSitsWriteErrorDate.orNull

  @Column(name = "last_sits_write_error")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.RecordedAssessmentComponentStudentMarkSitsErrorUserType")
  private var _lastSitsWriteError: RecordedAssessmentComponentStudentMarkSitsError = _
  def lastSitsWriteError: Option[RecordedAssessmentComponentStudentMarkSitsError] = Option(_lastSitsWriteError)
  def lastSitsWriteError_=(lastSitsWriteError: Option[RecordedAssessmentComponentStudentMarkSitsError]): Unit = _lastSitsWriteError = lastSitsWriteError.orNull

  def markWrittenToSits(): Unit = {
    needsWritingToSitsSince = None
    lastWrittenToSits = Some(DateTime.now)
    lastSitsWriteErrorDate = None
    lastSitsWriteError = None
  }

  def markWrittenToSitsError(error: RecordedAssessmentComponentStudentMarkSitsError): Unit = {
    needsWritingToSitsSince = None
    lastSitsWriteErrorDate = Some(DateTime.now)
    lastSitsWriteError = Some(error)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "moduleCode" -> moduleCode,
    "assessmentGroup" -> assessmentGroup,
    "occurrence" -> occurrence,
    "sequence" -> sequence,
    "academicYear" -> academicYear,
    "universityId" -> universityId,
    "assessmentType" -> assessmentType,
    "resitSequence" -> resitSequence,
    "marks" -> marks,
    "needsWritingToSitsSince" -> needsWritingToSitsSince,
    "lastWrittenToSits" -> lastWrittenToSits,
    "lastSitsWriteErrorDate" -> lastSitsWriteErrorDate,
    "lastSitsWriteError" -> lastSitsWriteError
  )
}

@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedAssessmentComponentStudentMark extends GeneratedId
  with ToString {

  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = Array())
  @JoinColumn(name = "recorded_assessment_component_student_id", nullable = false)
  @ForeignKey(name = "none")
  var recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var mark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var grade: Option[String] = None

  var comments: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.RecordedAssessmentComponentStudentMarkSourceUserType")
  var source: RecordedAssessmentComponentStudentMarkSource = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.MarkStateUserType")
  @Column(name = "mark_state")
  var markState: MarkState = UnconfirmedActual

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  @Column(name = "updated_by", nullable = false)
  var updatedBy: User = _

  @Column(name = "updated_date", nullable = false)
  var updatedDate: DateTime = _

  override def toStringProps: Seq[(String, Any)] = Seq(
    "mark" -> mark,
    "grade" -> grade,
    "comments" -> comments,
    "source" -> source
  )
}

sealed abstract class RecordedAssessmentComponentStudentMarkSitsError(val description: String) extends EnumEntry
object RecordedAssessmentComponentStudentMarkSitsError extends Enum[RecordedAssessmentComponentStudentMarkSitsError] {
  case object MissingMarksRecord extends RecordedAssessmentComponentStudentMarkSitsError("Missing SAS record for component mark")

  override def values: IndexedSeq[RecordedAssessmentComponentStudentMarkSitsError] = findValues
}

sealed abstract class RecordedAssessmentComponentStudentMarkSource(val description: String) extends EnumEntry
object RecordedAssessmentComponentStudentMarkSource extends Enum[RecordedAssessmentComponentStudentMarkSource] {
  case object MarkEntry extends RecordedAssessmentComponentStudentMarkSource("Record component marks")
  case object Scaling extends RecordedAssessmentComponentStudentMarkSource("Scaling")
  case object MissingMarkAdjustment extends RecordedAssessmentComponentStudentMarkSource("Missing mark adjustment")
  case object ModuleMarkConfirmation extends RecordedAssessmentComponentStudentMarkSource("Module mark confirmation")
  case object ProcessModuleMarks extends RecordedAssessmentComponentStudentMarkSource("Process module marks")
  case object ProcessCohortMarks extends RecordedAssessmentComponentStudentMarkSource("Process cohort marks")
  case object CourseworkMarking extends RecordedAssessmentComponentStudentMarkSource("Coursework marking")
  case object SyncFromSITS extends RecordedAssessmentComponentStudentMarkSource("Sync from SITS")

  override def values: IndexedSeq[RecordedAssessmentComponentStudentMarkSource] = findValues
}

class RecordedAssessmentComponentStudentMarkSourceUserType extends EnumUserType(RecordedAssessmentComponentStudentMarkSource)
class RecordedAssessmentComponentStudentMarkSitsErrorUserType extends EnumUserType(RecordedAssessmentComponentStudentMarkSitsError)

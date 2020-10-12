package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
 * A set of module marks for a student on a particular Module, as recorded in Tabula. This is
 * *NOT* the canonical module mark, the canonical mark should use the fields on ModuleRegistration, but it
 * is guaranteed to be persistent - it will not be removed if the module registration record is removed from SITS nor
 * will it be updated by updates in SITS; it is a permanent record of module marks that have been recorded in
 * Tabula for upload into SITS.
 */
@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedModuleRegistration extends GeneratedId
  with HibernateVersioned
  with ToEntityReference
  with ToString with Serializable {

  @transient var profileService: ProfileService = Wire.auto[ProfileService]

  override type Entity = RecordedModuleRegistration

  def this(mr: ModuleRegistration) = {
    this()
    this.sprCode = mr.sprCode
    this.sitsModuleCode = mr.sitsModuleCode
    this.academicYear = mr.academicYear
    this.occurrence = mr.occurrence
  }

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  // This isn't really a ManyToMany but we don't have bytecode instrumentation so this allows us to make it lazy at both ends
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "ModuleRegistration_RecordedModuleRegistration",
    joinColumns = Array(new JoinColumn(name = "recorded_module_registration_id", insertable = false, updatable = false)),
    inverseJoinColumns = Array(new JoinColumn(name = "module_registration_id", insertable = false, updatable = false))
  )
  @JoinColumn(name = "id", insertable = false, updatable = false)
  @BatchSize(size = 200)
  private val _moduleRegistration: JSet[ModuleRegistration] = JHashSet()
  def moduleRegistration: Option[ModuleRegistration] = _moduleRegistration.asScala.headOption

  @Column(name = "sits_module_code", nullable = false)
  var sitsModuleCode: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(nullable = false)
  var occurrence: String = _

  @OneToMany(mappedBy = "recordedModuleRegistration", cascade = Array(ALL), fetch = FetchType.EAGER)
  @OrderBy("updated_date DESC")
  @BatchSize(size = 200)
  private val _marks: JList[RecordedModuleMark] = JArrayList()
  def marks: Seq[RecordedModuleMark] = _marks.asScala.toSeq

  def addMark(
    uploader: User,
    mark: Option[Int],
    grade: Option[String],
    result: Option[ModuleResult],
    source: RecordedModuleMarkSource,
    markState: MarkState,
    comments: String = null
  ): RecordedModuleMark = {
    val newMark = new RecordedModuleMark
    newMark.recordedModuleRegistration = this
    newMark.mark = mark
    newMark.grade = grade
    newMark.result = result
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
  def latestResult: Option[ModuleResult] = marks.headOption.flatMap(_.result)
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
  @Type(`type` = "uk.ac.warwick.tabula.data.model.RecordedModuleMarkSitsErrorUserType")
  private var _lastSitsWriteError: RecordedModuleMarkSitsError = _
  def lastSitsWriteError: Option[RecordedModuleMarkSitsError] = Option(_lastSitsWriteError)
  def lastSitsWriteError_=(lastSitsWriteError: Option[RecordedModuleMarkSitsError]): Unit = _lastSitsWriteError = lastSitsWriteError.orNull

  def markWrittenToSits(): Unit = {
    needsWritingToSitsSince = None
    lastWrittenToSits = Some(DateTime.now)
    lastSitsWriteErrorDate = None
    lastSitsWriteError = None
  }

  def markWrittenToSitsError(error: RecordedModuleMarkSitsError): Unit = {
    needsWritingToSitsSince = None
    lastSitsWriteErrorDate = Some(DateTime.now)
    lastSitsWriteError = Some(error)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "sitsModuleCode" -> sitsModuleCode,
    "academicYear" -> academicYear,
    "occurrence" -> occurrence,
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
class RecordedModuleMark extends GeneratedId
  with ToString {

  @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = Array())
  @JoinColumn(name = "recorded_module_registration_id", nullable = false)
  @ForeignKey(name = "none")
  var recordedModuleRegistration: RecordedModuleRegistration = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var mark: Option[Int] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var grade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionModuleResultUserType")
  @Column(name = "module_result")
  var result: Option[ModuleResult] = _

  var comments: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.RecordedModuleMarkSourceUserType")
  var source: RecordedModuleMarkSource = _

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
    "result" -> result,
    "comments" -> comments,
    "source" -> source
  )
}

sealed abstract class RecordedModuleMarkSource(val description: String) extends EnumEntry
object RecordedModuleMarkSource extends Enum[RecordedModuleMarkSource] {
  case object ComponentMarkCalculation extends RecordedModuleMarkSource("Calculate module marks")
  case object RecordModuleMarks extends RecordedModuleMarkSource("Record module marks") // Used when the calculation is ignored
  case object MarkConfirmation extends RecordedModuleMarkSource("Confirm module marks")
  case object ProcessModuleMarks extends RecordedModuleMarkSource("Process module marks")
  case object ProcessCohortMarks extends RecordedModuleMarkSource("Process cohort marks")
  case object ComponentMarkChange extends RecordedModuleMarkSource("Component mark changed")
  case object SyncFromSITS extends RecordedModuleMarkSource("Sync from SITS")

  override def values: IndexedSeq[RecordedModuleMarkSource] = findValues
}

sealed abstract class RecordedModuleMarkSitsError(val description: String) extends EnumEntry
object RecordedModuleMarkSitsError extends Enum[RecordedModuleMarkSitsError] {
  case object MissingMarksRecord extends RecordedModuleMarkSitsError("Missing SMR record for module registration")
  case object MissingModuleRegistration extends RecordedModuleMarkSitsError("Missing SMO record for module registration")

  override def values: IndexedSeq[RecordedModuleMarkSitsError] = findValues
}

class RecordedModuleMarkSourceUserType extends EnumUserType(RecordedModuleMarkSource)
class RecordedModuleMarkSitsErrorUserType extends EnumUserType(RecordedModuleMarkSitsError)

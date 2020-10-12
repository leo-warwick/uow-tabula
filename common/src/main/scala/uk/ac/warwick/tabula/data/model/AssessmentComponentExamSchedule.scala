package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType.ALL
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

@Entity
@Proxy
class AssessmentComponentExamSchedule extends GeneratedId with ToString {

  def this(examProfileCode: String, slotId: String, sequence: String, locationSequence: String) = {
    this()
    this.examProfileCode = examProfileCode
    this.slotId = slotId
    this.sequence = sequence
    this.locationSequence = locationSequence
  }

  // moduleCode and assessmentComponentSequence are a many-to-one mapping to an AssessmentComponent
  @Column(name = "module_code", nullable = false)
  var moduleCode: String = _

  @Column(name = "assessmentcomponent_sequence", nullable = false)
  var assessmentComponentSequence: String = _

  // A schedule is unique on examProfileCode, slotId, sequence and locationSequence
  @Column(name = "profile_code", nullable = false)
  var examProfileCode: String = _

  @Column(name = "slot_id", nullable = false)
  var slotId: String = _

  @Column(name = "sequence", nullable = false)
  var sequence: String = _

  @Column(name = "location_sequence", nullable = false)
  var locationSequence: String = _

  // This is the academic year of the users on the assessment, used for linking to UpstreamAssessmentGroup (along with moduleCode and assessmentComponentSequence)
  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "start_time", nullable = false)
  var startTime: DateTime = _

  @Column(name = "exam_paper_code", nullable = false)
  var examPaperCode: String = _

  @Column(name = "exam_paper_section", nullable = true)
  private var _examPaperSection: String = _
  def examPaperSection: Option[String] = _examPaperSection.maybeText
  def examPaperSection_=(section: Option[String]): Unit = _examPaperSection = section.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.LocationUserType")
  @Column(name = "location", nullable = true)
  private var _location: Location = _
  def location: Option[Location] = Option(_location)
  def location_=(location: Option[Location]): Unit = _location = location.orNull

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(ALL))
  @JoinColumn(name = "schedule_id")
  @BatchSize(size = 200)
  @OrderBy("seat_number, university_id")
  var students: JList[AssessmentComponentExamScheduleStudent] = JArrayList()

  // set of all occurrences of a module that students sitting this exam are registered on - needed for linking with UpstreamAssessmentGroup
  def occurrences: Set[String] = students.asScala.map(_.occurrence).toSet

  def copyFrom(other: AssessmentComponentExamSchedule): AssessmentComponentExamSchedule = {
    require(other.id == null, "Can only copy from transient instances")
    require(other.examProfileCode == examProfileCode && other.slotId == slotId && other.sequence == sequence && other.locationSequence == locationSequence, "Must be for the same exam profile, slot, sequence in slot and location sequence")
    this.moduleCode = other.moduleCode
    this.assessmentComponentSequence = other.assessmentComponentSequence
    this.academicYear = other.academicYear
    this.startTime = other.startTime
    this.examPaperCode = other.examPaperCode
    this._examPaperSection = other._examPaperSection
    this._location = other._location
    this
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "moduleCode" -> moduleCode,
    "assessmentComponentSequence" -> assessmentComponentSequence,
    "examProfileCode" -> examProfileCode,
    "slotId" -> slotId,
    "sequence" -> sequence,
    "locationSequence" -> locationSequence,
    "academicYear" -> academicYear,
    "startTime" -> startTime,
    "examPaperCode" -> examPaperCode,
    "examPaperSection" -> _examPaperSection,
    "location" -> _location
  )
}

package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString

@Entity
@Proxy
class AssessmentComponentExamScheduleStudent extends GeneratedId with ToString {

  def this(schedule: AssessmentComponentExamSchedule) = {
    this()
    this.schedule = schedule
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "schedule_id", insertable = false, updatable = false)
  var schedule: AssessmentComponentExamSchedule = _

  @Column(name = "seat_number", nullable = true)
  private var _seatNumber: JInteger = _
  def seatNumber: Option[Int] = Option(_seatNumber)
  def seatNumber_=(seat: Option[Int]): Unit = _seatNumber = JInteger(seat)

  @Column(name = "university_id", nullable = false)
  var universityId: String = _

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumn(name = "university_id", referencedColumnName = "universityId", insertable = false, updatable = false)
  private val _student: StudentMember = null
  def studentCourseDetails: Option[StudentCourseDetails] = Option(_student).flatMap(_.mostSignificantCourseDetails)

  // SITS MAV occurrence
  @Column(name = "occurrence", nullable = false)
  var occurrence: String = _

  def copyFrom(other: AssessmentComponentExamScheduleStudent): AssessmentComponentExamScheduleStudent = {
    require(other.id == null, "Can only copy from transient instances")
    _seatNumber = other._seatNumber
    universityId = other.universityId
    sprCode = other.sprCode
    occurrence = other.occurrence
    this
  }

  def sameDataAs(other: AssessmentComponentExamScheduleStudent): Boolean =
    _seatNumber == other._seatNumber &&
    universityId == other.universityId &&
    sprCode == other.sprCode &&
    occurrence == other.occurrence

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "schedule" -> schedule,
    "seatNumber" -> _seatNumber,
    "universityId" -> universityId,
    "sprCode" -> sprCode,
    "occurrence" -> occurrence
  )
}

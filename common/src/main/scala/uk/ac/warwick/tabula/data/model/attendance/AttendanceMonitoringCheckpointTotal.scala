package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

@Entity
@Proxy
@Table(name = "ATTENDANCEMONITORINGTOTAL")
class AttendanceMonitoringCheckpointTotal extends GeneratedId with ToEntityReference {

  def this(student: StudentMember, department: Department, academicYear: AcademicYear) = {
    this()
    this.student = student
    this.department = department
    this.academicYear = academicYear
  }

  type Entity = AttendanceMonitoringCheckpointTotal

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id")
  var student: StudentMember = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @NotNull
  @Column(name = "academicyear")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  var academicYear: AcademicYear = _

  @NotNull
  var unrecorded: Int = 0

  @NotNull
  @Column(name = "authorized")
  var authorised: Int = 0

  @NotNull
  @Column(name = "unauthorized")
  var unauthorised: Int = 0

  @NotNull
  var attended: Int = 0

  @NotNull
  @Column(name = "updated_date")
  var updatedDate: DateTime = new DateTime(0)

  @Column(name = "low_level_notified")
  var unauthorisedLowLevelNotified: DateTime = _

  @Column(name = "medium_level_notified")
  var unauthorisedMediumLevelNotified: DateTime = _

  @Column(name = "high_level_notified")
  var unauthorisedHighLevelNotified: DateTime = _

}

package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.{Min, NotNull}
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

@Entity
@Proxy(`lazy` = false)
class MonitoringPointReport extends GeneratedId with ToEntityReference {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student", referencedColumnName = "universityId")
  var student: StudentMember = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_course_details_id")
  var studentCourseDetails: StudentCourseDetails = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_course_year_details_id")
  var studentCourseYearDetails: StudentCourseYearDetails = _

  @NotNull
  var createdDate: DateTime = _

  // CAN be null
  var pushedDate: DateTime = _

  @NotNull
  var monitoringPeriod: String = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = _

  @NotNull
  @Min(1)
  var missed: Int = _

  @NotNull
  var reporter: String = _

  override type Entity = MonitoringPointReport
}
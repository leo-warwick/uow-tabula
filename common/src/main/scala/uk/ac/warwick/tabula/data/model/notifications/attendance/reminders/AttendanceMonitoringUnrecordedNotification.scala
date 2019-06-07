package uk.ac.warwick.tabula.data.model.notifications.attendance.reminders

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.userlookup.User

abstract class AbstractAttendanceMonitoringUnrecordedNotification
  extends Notification[Department, Unit]
    with SingleItemNotification[Department]
    with AllCompletedActionRequiredNotification {

  override final def verb = "record"

  priority = NotificationPriority.Critical

  @transient
  var attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]

  @transient
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  final def referenceDate: DateTime = created.plusDays(-7)

  @transient
  lazy val academicYear: AcademicYear = AcademicYear.forDate(referenceDate)

  final def department: Department = item.entity

}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue(value = "AttendanceMonitoringUnrecordedPoints")
class AttendanceMonitoringUnrecordedPointsNotification
  extends AbstractAttendanceMonitoringUnrecordedNotification {

  override final def url: String = Routes.View.pointsUnrecorded(department, academicYear)

  override final def urlTitle = "record attendance for these points"

  override def title: String = {
    val pointsCount = unrecordedPoints.groupBy(p => (p.name, p.startDate, p.endDate)).size

    s"$pointsCount monitoring ${if (pointsCount == 1) "point needs" else "points need"} recording"
  }

  final def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unrecorded_points_notification.ftl"

  @transient
  final lazy val unrecordedPoints: Seq[AttendanceMonitoringPoint] = {
    attendanceMonitoringService.findUnrecordedPoints(department, academicYear, referenceDate.toLocalDate)
  }

  override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
    "department" -> department,
    "academicYear" -> academicYear,
    "points" -> unrecordedPoints.groupBy(p => (p.name, p.startDate, p.endDate)).map { case (_, groupedPoints) => groupedPoints.head }
  ))

  override final def recipients: Seq[User] = {
    if (unrecordedPoints.nonEmpty) {
      // department.owners is not populated correctly if department not fetched directly
      moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users.toSeq
    } else {
      Seq()
    }
  }

}

@Entity
@Proxy(`lazy` = false)
@DiscriminatorValue(value = "AttendanceMonitoringUnrecordedStudents")
class AttendanceMonitoringUnrecordedStudentsNotification
  extends AbstractAttendanceMonitoringUnrecordedNotification {

  override final def url: String = Routes.View.studentsUnrecorded(department, academicYear)

  override final def urlTitle = "record attendance for these students"

  override def title: String = {
    val studentsCount = unrecordedStudents.size

    s"$studentsCount ${if (studentsCount == 1) "student needs" else "students need"} monitoring points recording"
  }

  final def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_unrecorded_students_notification.ftl"

  @transient
  final lazy val unrecordedStudents: Seq[AttendanceMonitoringStudentData] = {
    attendanceMonitoringService.findUnrecordedStudents(department, academicYear, created.toLocalDate)
      .sortBy(u => (u.lastName, u.firstName))
  }

  override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
    "department" -> department,
    "academicYear" -> academicYear,
    "students" -> unrecordedStudents,
    "truncatedStudents" -> unrecordedStudents.slice(0, 10)
  ))

  @transient
  override def recipients: Seq[User] =
    if (unrecordedStudents.nonEmpty) {
      // department.owners is not populated correctly if department not fetched directly
      moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users.toSeq
    } else {
      Seq()
    }
}
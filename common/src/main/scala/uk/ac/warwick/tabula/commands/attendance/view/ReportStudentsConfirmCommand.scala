package uk.ac.warwick.tabula.commands.attendance.view

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.data.model.notifications.ReportStudentsConfirmNotification
import uk.ac.warwick.tabula.data.model.{Department, Notification, StudentMember}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

object ReportStudentsConfirmCommand {
  def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
    new ReportStudentsConfirmCommandInternal(department, academicYear, user)
      with ComposableCommand[Seq[MonitoringPointReport]]
      with AutowiringAttendanceMonitoringServiceComponent
      with AutowiringProfileServiceComponent
      with ReportStudentsConfirmValidation
      with ReportStudentsConfirmDescription
      with ReportStudentsConfirmNotifier
      with ReportStudentsConfirmPermissions
      with ReportStudentsConfirmCommandState
}

class ReportStudentsConfirmCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
  extends CommandInternal[Seq[MonitoringPointReport]] {

  self: ReportStudentsConfirmCommandState with AttendanceMonitoringServiceComponent =>

  override def applyInternal(): Seq[MonitoringPointReport] = {
    studentMissedReportCounts.map { src => {
      val scyd = src.student.freshStudentCourseYearDetailsForYear(academicYear).getOrElse(throw new IllegalArgumentException())
      val report = new MonitoringPointReport
      report.academicYear = academicYear
      report.createdDate = DateTime.now
      report.missed = src.missed
      report.monitoringPeriod = period
      report.reporter = user.departmentCode.toUpperCase + user.apparentUser.getWarwickId
      report.student = src.student
      report.studentCourseDetails = scyd.studentCourseDetails
      report.studentCourseYearDetails = scyd
      attendanceMonitoringService.saveOrUpdate(report)
      report
    }
    }
  }
}

trait ReportStudentsConfirmNotifier extends Notifies[Seq[MonitoringPointReport], User] {

  self: AttendanceMonitoringServiceComponent with ReportStudentsConfirmCommandState =>
  override def emit(result: Seq[MonitoringPointReport]): Seq[ReportStudentsConfirmNotification] = {
    Seq(Notification.init(new ReportStudentsConfirmNotification, user.apparentUser, result))
  }
}

trait ReportStudentsConfirmValidation extends SelfValidating {

  self: ReportStudentsConfirmCommandState with AttendanceMonitoringServiceComponent =>

  override def validate(errors: Errors) {
    if (!availablePeriods.filter(_._2).map(_._1).contains(period)) {
      errors.rejectValue("availablePeriods", "attendanceMonitoringReport.invalidPeriod")
    }
    if (studentMissedReportCounts.isEmpty) {
      errors.rejectValue("studentMissedReportCounts", "attendanceMonitoringReport.noStudents")
    }
    if (!confirm) {
      errors.rejectValue("confirm", "attendanceMonitoringReport.confirm")
    }
  }

}

trait ReportStudentsConfirmPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: ReportStudentsConfirmCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MonitoringPoints.Report, department)
  }

}

trait ReportStudentsConfirmDescription extends Describable[Seq[MonitoringPointReport]] {

  self: ReportStudentsConfirmCommandState =>

  override lazy val eventName = "ReportStudentsConfirm"

  override def describe(d: Description) {
    d.property("monitoringPeriod", period)
    d.property("academicYear", academicYear.toString)
    d.property("students", studentMissedReportCounts.map { src => src.student.userId -> src.missed }.toMap)
  }
}

trait ReportStudentsConfirmCommandState extends ReportStudentsChoosePeriodCommandState {

  self: AttendanceMonitoringServiceComponent =>

  def user: CurrentUser

  lazy val currentAcademicYear: AcademicYear = AcademicYear.now()
  lazy val currentPeriod: String = currentAcademicYear.termOrVacationForDate(LocalDate.now).periodType.toString

  // Bind variables
  var students: JList[StudentMember] = LazyLists.create()
  var filterString: String = _
  var confirm: Boolean = false
  override lazy val allStudents: mutable.Buffer[StudentMember] = students.asScala
}

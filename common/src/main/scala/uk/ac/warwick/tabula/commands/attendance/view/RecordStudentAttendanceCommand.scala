package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object RecordStudentAttendanceCommand {

	def apply(department: Department, academicYear: AcademicYear, student: StudentMember, user: CurrentUser) =
		new RecordStudentAttendanceCommandInternal(department, academicYear, student, user)
			with StudentRecordCommandHelper
			with RecordStudentAttendanceDescription
			with RecordStudentAttendanceCommandState
}


class RecordStudentAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val student: StudentMember, val user: CurrentUser)
	extends CommandInternal[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

	self: StudentRecordCommandRequest with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		attendanceMonitoringService.setAttendance(student, checkpointMap.asScala.toMap, user)
	}

}

trait RecordStudentAttendanceDescription extends Describable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

	self: RecordStudentAttendanceCommandState =>

	override lazy val eventName = "RecordStudentAttendance"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
	}
}

trait RecordStudentAttendanceCommandState extends StudentRecordCommandState {

	self: AttendanceMonitoringServiceComponent =>

	def department: Department
	override def departmentOption: Option[Department] = Option(department)

}

package uk.ac.warwick.tabula.commands.attendance

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.view.{FiltersCheckpointMapChanges, MissedAttendanceMonitoringCheckpointsNotifications}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

trait StudentRecordCommandHelper
	extends ComposableCommand[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])]
	with PopulatesStudentRecordCommand
	with AutowiringAttendanceMonitoringServiceComponent
	with AutowiringTermServiceComponent
	with StudentRecordValidation
	with StudentRecordPermissions
	with StudentRecordCommandRequest
	with MissedAttendanceMonitoringCheckpointsNotifications {

	self: CommandInternal[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])]
		with StudentRecordCommandRequest
		with StudentRecordCommandState =>
}

object StudentRecordCommand {
	def apply(academicYear: AcademicYear, student: StudentMember, user: CurrentUser) =
		new StudentRecordCommandInternal(academicYear, student, user)
			with StudentRecordCommandHelper
			with StudentRecordDescription
			with StudentRecordCommandState

}


class StudentRecordCommandInternal(val academicYear: AcademicYear, val student: StudentMember, val user: CurrentUser)
	extends CommandInternal[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

	self: StudentRecordCommandRequest with StudentRecordCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		attendanceMonitoringService.setAttendance(student, checkpointMap.asScala.toMap, user)
	}

}

trait PopulatesStudentRecordCommand extends PopulateOnForm {

	self: StudentRecordCommandRequest with StudentRecordCommandState with AttendanceMonitoringServiceComponent =>

	override def populate(): Unit = {
		studentPointCheckpointMap(student).foreach { case (point, checkpoint) => checkpointMap.put(point, Option(checkpoint).map(_.state).orNull) }
	}
}

trait StudentRecordValidation extends SelfValidating with FiltersCheckpointMapChanges {

	self: StudentRecordCommandRequest with StudentRecordCommandState
		with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def validate(errors: Errors): Unit = {
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)

		filterCheckpointMapForChanges(
			Map(student -> checkpointMap.asScala.toMap),
			studentPointCheckpointMap.mapValues(_.mapValues(Option(_).map(_.state).orNull))
		).getOrElse(student, Map()).foreach { case (point, state) =>
			errors.pushNestedPath(s"checkpointMap[${point.id}]")

			if (!points.contains(point)) {
				// should never be the case, but protect against POST-hack
				errors.rejectValue("","Submitted attendance for a point which is student is not attending")
			}

			if (!nonReportedTerms.contains(termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)) {
				errors.rejectValue("", "attendanceMonitoringCheckpoint.alreadyReportedThisTerm")
			}

			if (point.isStartDateInFuture && !(state == null || state == AttendanceState.MissedAuthorised)) {
				if (state == AttendanceState.MissedUnauthorised) errors.rejectValue("", "monitoringCheckpoint.missedUnauthorised.beforeStart")
				else if (state == AttendanceState.Attended) errors.rejectValue("", "monitoringCheckpoint.attended.beforeStart")
			}

			// check that a note exists for authorised absences
			if (state == AttendanceState.MissedAuthorised && attendanceMonitoringService.getAttendanceNote(student, point).isEmpty) {
				errors.rejectValue("", "monitoringCheckpoint.missedAuthorised.noNote")
			}

			errors.popNestedPath()
		}
	}

}

trait StudentRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentRecordCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}

}

trait StudentRecordDescription extends Describable[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])] {

	self: StudentRecordCommandState =>

	override lazy val eventName = "StudentRecord"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
	}
}

trait StudentRecordCommandState {

	self: AttendanceMonitoringServiceComponent =>

	def academicYear: AcademicYear
	def student: StudentMember
	def user: CurrentUser

	def departmentOption: Option[Department] = None

	lazy val points: Seq[AttendanceMonitoringPoint] = attendanceMonitoringService.listStudentsPoints(student, departmentOption, academicYear)

	lazy val studentPointCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] = {
		val checkpoints = attendanceMonitoringService.getCheckpoints(points, student)
		Map(student -> points.map(p => p -> checkpoints.getOrElse(p, null)).toMap)
	}

}

trait StudentRecordCommandRequest {
	var checkpointMap: JMap[AttendanceMonitoringPoint, AttendanceState] = JHashMap()
}

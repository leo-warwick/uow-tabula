package uk.ac.warwick.tabula.commands.attendance.view

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, TermServiceComponent}

trait FiltersCheckpointMapChanges {

	def filterCheckpointMapForChanges(
		changedCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]],
		existingCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]]
	): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]] = {
		val flattenedChanges: Seq[(StudentMember, AttendanceMonitoringPoint, AttendanceState)] =
			changedCheckpointMap.mapValues(_.toSeq).toSeq.flatMap { case (student, pointCheckpoints) => pointCheckpoints.map { case (point, state) => (student, point, state) } }
		val changes = flattenedChanges.filter { case (student, point, state) =>
			!existingCheckpointMap.get(student).flatMap(_.get(point)).contains(state)
		}
		changes.groupBy { case (student, _, _) => student }
			.mapValues(_.groupBy { case (_ , point, _) => point }
				.mapValues(_.map { case (_, _, state) => state }.head)
			)
	}
}

trait GroupedPointRecordValidation extends FiltersCheckpointMapChanges {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent with SecurityServiceComponent =>

	def validateGroupedPoint(
		errors: Errors,
		templatePoint: AttendanceMonitoringPoint,
		checkpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]],
		existingCheckpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]],
		user: CurrentUser
	): Unit = {
		filterCheckpointMapForChanges(checkpointMap, existingCheckpointMap).foreach{ case(student, pointMap) =>
			pointMap.foreach{ case(point, state) =>
				errors.pushNestedPath(s"checkpointMap[${student.universityId}][${point.id}]")
				// Check point is valid for student
				if (point.scheme.department != templatePoint.scheme.department && !securityService.can(user, Permissions.MonitoringPoints.Record, student)
					|| !point.scheme.members.includesUser(MemberOrUser(student).asUser)
				) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
				} else {
					// Check not reported
					if (!attendanceMonitoringService.findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
						termService.getTermFromDateIncludingVacations(templatePoint.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)
					){
						errors.rejectValue("", "monitoringCheckpoint.student.alreadyReportedThisTerm")
					}

					// Check valid state
					if (DateTime.now.isBefore(point.startDate.toDateTimeAtStartOfDay) && !(state == null || state == AttendanceState.MissedAuthorised)) {
						if (state == AttendanceState.MissedUnauthorised) errors.rejectValue("", "monitoringCheckpoint.missedUnauthorised.beforeStart")
						else if (state == AttendanceState.Attended) errors.rejectValue("", "monitoringCheckpoint.attended.beforeStart")
					}

					// check that a note exists for authorised absences
					if (state == AttendanceState.MissedAuthorised && attendanceMonitoringService.getAttendanceNote(student, point).isEmpty) {
						errors.rejectValue("", "monitoringCheckpoint.missedAuthorised.noNote")
					}
				}
				errors.popNestedPath()
			}}
		}
}
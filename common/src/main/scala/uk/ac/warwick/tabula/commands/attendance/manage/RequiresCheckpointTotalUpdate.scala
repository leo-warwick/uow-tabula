package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

trait RequiresCheckpointTotalUpdate {

	self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	def updateCheckpointTotals(universityIds: Seq[String], department: Department, academicYear: AcademicYear): Unit = {
		if (universityIds.nonEmpty) {
			val students = profileService.getAllMembersWithUniversityIds(universityIds).flatMap {
				case student: StudentMember => Option(student)
				case _ => None
			}
			if (students.nonEmpty) {
				attendanceMonitoringService.setCheckpointTotalsForUpdate(students, department, academicYear)
			}
		}
	}

	/**
		* This method should not be used when the scheme's students have changed
		* as the previous students won't be updated
		*/
	def updateCheckpointTotals(scheme: AttendanceMonitoringScheme): Unit = {
		updateCheckpointTotals(scheme.members.members, scheme.department, scheme.academicYear)
	}

	/**
		* This method should not be used when the scheme's students have changed
		* as the previous students won't be updated
		*/
	def updateCheckpointTotals(schemes: Seq[AttendanceMonitoringScheme]): Unit = {
		schemes.groupBy(s => (s.department, s.academicYear)).foreach { case ((department, academicYear), groupedSchemes) =>
			updateCheckpointTotals(groupedSchemes.flatMap(_.members.members).distinct, department, academicYear)
		}
	}

}

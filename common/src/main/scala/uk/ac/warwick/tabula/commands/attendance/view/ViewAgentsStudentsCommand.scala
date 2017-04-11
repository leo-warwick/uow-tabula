package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ViewAgentsStudentsCommand {
	def apply(department: Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType, agent: Member) =
		new ViewAgentsStudentsCommandInternal(department, academicYear, relationshipType, agent)
			with ViewAgentsStudentsPermissions
			with AutowiringRelationshipServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[FilteredStudentsAttendanceResult]
			with ViewAgentsStudentsState
			with ReadOnly with Unaudited
 }

class ViewAgentsStudentsCommandInternal(val department: Department, val academicYear: AcademicYear, val relationshipType: StudentRelationshipType, val agent: Member)
	extends CommandInternal[FilteredStudentsAttendanceResult] with BuildsFilteredStudentsAttendanceResult {

		self: RelationshipServiceComponent with AttendanceMonitoringServiceComponent with TermServiceComponent =>

		def applyInternal(): FilteredStudentsAttendanceResult = {
			val students = relationshipService.listCurrentStudentRelationshipsWithMemberInDepartment(relationshipType, agent, department).flatMap(_.studentMember)
			buildAttendanceResult(students.size, students, Option(department), academicYear)
		}
	}

trait ViewAgentsStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	 self: ViewAgentsStudentsState =>

	 def permissionsCheck(p: PermissionsChecking) {
		 p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	 }
 }

trait ViewAgentsStudentsState {
	def department: Department
	def academicYear: AcademicYear
	def relationshipType: StudentRelationshipType
	def agent: Member
}
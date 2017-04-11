package uk.ac.warwick.tabula.commands.reports.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ProfileExportCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new ProfileExportCommandInternal(department, academicYear, user)
			with AutowiringSecurityServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Seq[AttendanceMonitoringStudentData]]
			with Unaudited with ReadOnly
			with ProfileExportPermissions
			with ProfileExportCommandState
}


class ProfileExportCommandInternal(val department: Department, val academicYear: AcademicYear, user: CurrentUser)
	extends CommandInternal[Seq[AttendanceMonitoringStudentData]] with TaskBenchmarking {

	self: ProfileServiceComponent with ProfileExportCommandState with SecurityServiceComponent =>

	override def applyInternal(): Seq[AttendanceMonitoringStudentData] = {
		val result = {
			if (searchSingle || searchMulti) {
				val members = {
					if (searchSingle) profileService.getAllMembersWithUserId(singleSearch)
					else profileService.getAllMembersWithUniversityIds(multiSearchEntries)
				}
				val students = members.flatMap {
					case student: StudentMember => Some(student)
					case _ => None
				}
				val (validStudents, noPermission) = students.partition(s => securityService.can(user, Permissions.Department.Reports, s))

				notFoundIds = multiSearchEntries.diff(students.map(_.universityId))
				noPermissionIds = noPermission.map(_.universityId)

				validStudents.map(s => {
					val scd = Option(s.mostSignificantCourse)
					AttendanceMonitoringStudentData(
						s.firstName,
						s.lastName,
						s.universityId,
						s.userId,
						null,
						null,
						scd.map(_.currentRoute.code).getOrElse(""),
						scd.map(_.currentRoute.name).getOrElse(""),
						scd.map(_.latestStudentCourseYearDetails.yearOfStudy.toString).getOrElse(""),
						scd.map(_.sprCode).getOrElse("")
					)
				})
			} else if (hasBeenFiltered) {
				benchmarkTask("profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments") {
					profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(
						department = department,
						restrictions = buildRestrictions(academicYear),
						academicYear
					)
				}
			} else {
				Seq()
			}
		}
		result.sortBy(s => (s.lastName, s.firstName))
	}
}

trait ProfileExportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileExportCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, department)
	}

}

trait ProfileExportCommandState extends FiltersStudents {
	def department: Department
	def academicYear: AcademicYear

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm

	var noPermissionIds: Seq[String] = Seq()
	var notFoundIds: Seq[String] = Seq()
	// Bind variables

	var page = 1
	var sortOrder: JList[Order] = JArrayList()
	var hasBeenFiltered = false
	var searchSingle = false
	var searchMulti = false
	var singleSearch: String = _
	var multiSearch: String = _
	def multiSearchEntries: Seq[String] =
		if (multiSearch == null) Nil
		else multiSearch split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}

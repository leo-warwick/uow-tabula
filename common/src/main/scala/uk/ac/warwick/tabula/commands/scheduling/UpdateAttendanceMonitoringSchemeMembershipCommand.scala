package uk.ac.warwick.tabula.commands.scheduling

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.manage.RequiresCheckpointTotalUpdate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent}

object UpdateAttendanceMonitoringSchemeMembershipCommand {
	def apply() =
		new UpdateAttendanceMonitoringSchemeMembershipCommandInternal
			with ComposableCommand[Seq[AttendanceMonitoringScheme]]
			with AutowiringFeaturesComponent
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringDeserializesFilterImpl
			with UpdateAttendanceMonitoringSchemeMembershipDescription
			with UpdateAttendanceMonitoringSchemeMembershipPermissions
			with UpdateAttendanceMonitoringSchemeMembershipCommandState
}

class UpdateAttendanceMonitoringSchemeMembershipCommandInternal extends CommandInternal[Seq[AttendanceMonitoringScheme]]
	with Logging with TaskBenchmarking with RequiresCheckpointTotalUpdate {

	self: FeaturesComponent with AttendanceMonitoringServiceComponent with UpdateAttendanceMonitoringSchemeMembershipCommandState =>

	override def applyInternal(): Seq[AttendanceMonitoringScheme] = {
		// TODO: This all runs in one giant transaction but it shouldn't need to
		// However, whenever I try and narrow the transaction to only when it's needed,
		// hibernate isn't picking up the change in staticUserIds before the manual flush (UserGroup.scala line 86).
		// That means it doesn't think there are any deletions to do, so it doesn't bother to flush.
		// Than when you save the scheme it suddently sees them all, but does the inserts first,
		// so the DB constraints are violated.
		// Every place I put the transaction, other than around the whole thing, have the same problem,
		// so maybe at some point someone else will think of a way around this.
		val schemesToUpdate = attendanceMonitoringService.listSchemesForMembershipUpdate

		logger.info(s"${schemesToUpdate.size} schemes need membership updating")

		val universityIDsToUpdateWithDepartmentAndAcademicYear: Seq[(String, Department, AcademicYear)] = schemesToUpdate.flatMap {
			scheme => benchmarkTask(s"Update scheme ${scheme.id}") {
				val previousUniversityIds = scheme.members.members

				deserializeFilter(scheme.memberQuery)
				val staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
					profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
						department = scheme.department,
						restrictions = buildRestrictions(scheme.academicYear),
						orders = buildOrders()
					)
				}

				scheme.members.staticUserIds = staticStudentIds
				attendanceMonitoringService.saveOrUpdate(scheme)


				(previousUniversityIds ++ scheme.members.members).distinct.map((_, scheme.department, scheme.academicYear))

			}
		}.distinct

		logger.info(s"Updating ${universityIDsToUpdateWithDepartmentAndAcademicYear.size} student checkpoint totals")

		val dataToUpdate: Seq[((Department, AcademicYear), Seq[String])] = universityIDsToUpdateWithDepartmentAndAcademicYear
			.groupBy { case (_, dept, academicYear) => (dept, academicYear) }
			.mapValues(_.map { case (universityId, _, _) => universityId }).toSeq


		dataToUpdate.foreach { case ((dept, academicYear), universityIds) =>
			benchmark(s"Update checkpoint totals for ${universityIds.size} students in ${dept.code} ${academicYear.toString}") {
				updateCheckpointTotals(universityIds, dept, academicYear)
			}

		}

		schemesToUpdate
	}

}

trait UpdateAttendanceMonitoringSchemeMembershipPermissions extends RequiresPermissionsChecking {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.UpdateMembership)
	}

}

trait UpdateAttendanceMonitoringSchemeMembershipDescription extends Describable[Seq[AttendanceMonitoringScheme]] {

	override lazy val eventName = "UpdateAttendanceMonitoringSchemeMembership"

	override def describe(d: Description) {

	}
}

trait UpdateAttendanceMonitoringSchemeMembershipCommandState extends FiltersStudents with DeserializesFilter {
	val department = null // Needs to be defined, but never actually used
	val defaultOrder = Seq(asc("lastName"), asc("firstName"))
	var sortOrder: JList[Order] = JArrayList() // Never used

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

}

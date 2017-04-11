package uk.ac.warwick.tabula.commands.reports

import org.joda.time.{LocalDate, DateTime}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait ReportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReportCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, department)
	}

}

trait ReportCommandState {
	def department: Department
	def academicYear: AcademicYear
}

trait ReportCommandRequest {
	var startDate: LocalDate = DateTime.now.minusWeeks(2).toLocalDate
	var endDate: LocalDate = DateTime.now.plusWeeks(2).toLocalDate
}
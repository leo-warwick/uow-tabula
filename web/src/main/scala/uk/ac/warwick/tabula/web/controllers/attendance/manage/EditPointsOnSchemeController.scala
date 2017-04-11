package uk.ac.warwick.tabula.web.controllers.attendance.manage

import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.commands.attendance.manage.FindPointsResult
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.HasMonthNames

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/{scheme}/edit/points"))
class EditPointsOnSchemeController extends AbstractManageSchemePointsController with HasMonthNames {

	override protected def render(
		findCommandResult: FindPointsResult,
		scheme: AttendanceMonitoringScheme,
		points: JavaImports.JInteger,
		actionCompleted: String
	): Mav = {
		Mav("attendance/manage/editschemepoints",
			"findResult" -> findCommandResult,
			"allTypes" -> AttendanceMonitoringPointType.values,
			"allStyles" -> AttendanceMonitoringPointStyle.values,
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters,
			"newPoints" -> Option(points).getOrElse(0),
			"actionCompleted" -> actionCompleted
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(mandatory(scheme).academicYear),
			Breadcrumbs.Manage.DepartmentForYear(mandatory(scheme).department, mandatory(scheme).academicYear)
		)
	}
}

package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.attendance.manage.FindPointsResult
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringTemplate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.web.Mav


@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/addpoints/template/{templateScheme}"))
class ViewTemplateSchemePointsController extends AttendanceController with HasMonthNames with GroupsPoints with AutowiringTermServiceComponent {

	var attendanceService: AttendanceMonitoringService = Wire.auto[AttendanceMonitoringService]

	@ModelAttribute("command")
	def command(
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable academicYear: AcademicYear,
		@PathVariable department: Department
	): DepartmentFindPointsResult = {
		DepartmentFindPointsResult(mandatory(department), getGroupedPointsFromTemplate(mandatory(templateScheme), mandatory(academicYear)))
	}

	@RequestMapping
	def getTemplateSelection(
		@ModelAttribute("command") cmd: DepartmentFindPointsResult,
		@PathVariable templateScheme: AttendanceMonitoringTemplate,
		@PathVariable department: Department
	): Mav = {
		Mav("attendance/manage/_displayfindpointresults",
			"findResult" -> cmd.pointResult,
			"templateScheme" -> templateScheme,
			"command" -> cmd
		).noLayoutIf(ajax)
	}


	def getGroupedPointsFromTemplate(templateScheme: AttendanceMonitoringTemplate, academicYear: AcademicYear): FindPointsResult = {
		val points = attendanceService.generatePointsFromTemplateScheme(templateScheme, academicYear)
		templateScheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Week => FindPointsResult(groupByTerm(points), Map())
			case AttendanceMonitoringPointStyle.Date => FindPointsResult(Map(), groupByMonth(points))
			case _ => FindPointsResult(groupByTerm(points), groupByMonth(points))
		}
	}
}

case class DepartmentFindPointsResult(department: Department, pointResult: FindPointsResult)

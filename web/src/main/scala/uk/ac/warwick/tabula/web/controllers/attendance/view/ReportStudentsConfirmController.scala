package uk.ac.warwick.tabula.web.controllers.attendance.view

import java.net.URLDecoder
import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.view.ReportStudentsConfirmCommand
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/report/confirm"))
class ReportStudentsConfirmController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ReportStudentsConfirmCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("attendance/view/reportconfirm").crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Students(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("submit-confirm"))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam period: String,
		@RequestParam(value = "filterString", required = false) filterString: String
	): Mav = {
		if(errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			val reports = cmd.apply()
			val filterMap = filterString match {
				case null => Seq()
				case s: String => URLDecoder.decode(s, "UTF-8").split("&").toSeq.flatMap(p => {
					val keyValue = p.split("=")
					if (keyValue.size < 2) None
					else Option((keyValue(0), keyValue(1)))
				})
			}
			val redirectObjects = Seq(
				"reports" -> reports.size,
				"monitoringPeriod" -> period,
				"academicYear" -> academicYear
			) ++ filterMap
			Redirect(Routes.View.students(department, academicYear), redirectObjects: _*)
		}
	}

}

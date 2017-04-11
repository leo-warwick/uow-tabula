package uk.ac.warwick.tabula.web.controllers.attendance.view

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.view.{ReportStudentsChoosePeriodCommand, StudentReportCount}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/report"))
class ReportStudentsChoosePeriodController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ReportStudentsChoosePeriodCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[StudentReportCount]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("attendance/view/reportperiod").crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Students(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentReportCount]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if(errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			val studentMissedReportCounts = cmd.apply()
			Mav("attendance/view/reportstudents",
				"studentMissedReportCounts" -> studentMissedReportCounts,
				"unrecordedStudentsCount" -> studentMissedReportCounts.count(_.unrecorded > 0)
			).crumbs(
				Breadcrumbs.View.HomeForYear(academicYear),
				Breadcrumbs.View.DepartmentForYear(department, academicYear),
				Breadcrumbs.View.Students(department, academicYear)
			)
		}
	}

}

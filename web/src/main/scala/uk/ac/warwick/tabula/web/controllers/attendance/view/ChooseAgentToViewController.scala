package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/agents"))
class ChooseAgentToViewController extends AttendanceController {

	@RequestMapping
	def home(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		Mav("attendance/view/chooseagent").crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear)
		)
	}

}
package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.commands.attendance.view.ViewStudentAttendanceCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/students/{student}"))
class ViewStudentAttendanceController extends AttendanceController with HasMonthNames
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable student: StudentMember) =
		ViewStudentAttendanceCommand(mandatory(department), mandatory(academicYear), mandatory(student))

	@RequestMapping
	def home(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember,
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]]
	): Mav = {
		Mav("attendance/view/student",
			"groupedPointMap" -> cmd.apply()
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Students(department, academicYear)
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.View.student(department, year, student)): _*)
	}

}
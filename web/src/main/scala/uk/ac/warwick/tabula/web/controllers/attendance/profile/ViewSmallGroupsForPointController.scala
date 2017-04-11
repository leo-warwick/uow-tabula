package uk.ac.warwick.tabula.web.controllers.attendance.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.profile.{ViewSmallGroupsForPointCommand, ViewSmallGroupsForPointCommandResult}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/profile/{student}/{academicYear}/{point}/groups"))
class ViewSmallGroupsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear
	) =
		ViewSmallGroupsForPointCommand(
			mandatory(student),
			mandatory(point),
			ListStudentGroupAttendanceCommand(mandatory(student), mandatory(academicYear)).apply()
		)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[ViewSmallGroupsForPointCommandResult]): Mav = {
		val result = cmd.apply()
		Mav("attendance/profile/groups",
			"result" -> result,
			"currentMember" -> currentMember,
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}
}
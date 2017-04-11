package uk.ac.warwick.tabula.web.controllers.attendance.profile

import uk.ac.warwick.tabula.data.model.{MeetingFormat, MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.attendance.profile.ViewMeetingsForPointCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/profile/{student}/{academicYear}/{point}/meetings"))
class ViewMeetingsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember,	@PathVariable point: AttendanceMonitoringPoint) =
		ViewMeetingsForPointCommand(mandatory(student), mandatory(point))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[(MeetingRecord, Seq[String])]]): Mav = {
		val meetingsStatuses = cmd.apply()
		Mav("attendance/view/meetings",
			"meetingsStatuses" -> meetingsStatuses,
			"allMeetingFormats" -> MeetingFormat.members
		).noLayoutIf(ajax)
	}
}
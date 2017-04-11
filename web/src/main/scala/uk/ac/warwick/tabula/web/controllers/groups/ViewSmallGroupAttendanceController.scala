package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.web.Mav

@RequestMapping(value = Array("/groups/group/{group}/attendance"))
@Controller
class ViewSmallGroupAttendanceController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable group: SmallGroup) = ViewSmallGroupAttendanceCommand(mandatory(group))

	@RequestMapping
	def show(
		@ModelAttribute("command") command: Appliable[ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation],
		@PathVariable group: SmallGroup
	): Mav = {
		val attendanceInfo = command.apply()
		val module = group.groupSet.module

		Mav("groups/attendance/view_group",
			"instances" -> attendanceInfo.instances,
			"studentAttendance" -> attendanceInfo.attendance,
			"attendanceNotes" -> attendanceInfo.notes
		).crumbs(Breadcrumbs.Department(module.adminDepartment, group.groupSet.academicYear), Breadcrumbs.ModuleForYear(module, group.groupSet.academicYear))
	}

}

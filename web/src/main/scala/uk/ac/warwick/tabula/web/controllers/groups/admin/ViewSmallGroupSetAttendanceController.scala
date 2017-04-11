package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ RequestMapping, PathVariable, ModelAttribute }
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.ViewSmallGroupSetAttendanceCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import scala.collection.immutable.SortedMap

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/attendance"))
@Controller
class ViewSmallGroupSetAttendanceController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable set: SmallGroupSet) = ViewSmallGroupSetAttendanceCommand(set)

	@RequestMapping
	def show(
		@ModelAttribute("command") command: Appliable[SortedMap[SmallGroup, ViewSmallGroupAttendanceCommand.SmallGroupAttendanceInformation]],
		@PathVariable set: SmallGroupSet
	): Mav = {
		val groups = command.apply()
		val module = set.module

		Mav("groups/attendance/view_set",
			"groups" -> groups
		).crumbs(Breadcrumbs.Department(module.adminDepartment, set.academicYear), Breadcrumbs.ModuleForYear(module, set.academicYear))
	}

}

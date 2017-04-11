package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{ListSmallGroupSetTimetableClashStudentsCommand, SmallGroupSetTimetableClashCommand}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/groups/{smallGroupSet}/timetableclash"))
class SmallGroupSetTimetableClashController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet) =
		SmallGroupSetTimetableClashCommand(mandatory(smallGroupSet))

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[Seq[(SmallGroup, Seq[User])]]): Mav = {
		val clashInfo = command.apply().map { case(group, users) => (group.id, users.map(user => user.getUserId)) }
		Mav(new JSONView(Map("students" -> clashInfo)))
	}
}

@Controller
@RequestMapping(value=Array("/groups/{smallGroupSet}/timetableclashstudentspopup"))
class ListSmallGroupSetTimetableClashStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet) =
		ListSmallGroupSetTimetableClashStudentsCommand(mandatory(smallGroupSet))

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[Seq[Member]]): Mav = {
		Mav("groups/timetableconflicts_students",
			"students" -> command.apply()
		).noLayout()
	}
}





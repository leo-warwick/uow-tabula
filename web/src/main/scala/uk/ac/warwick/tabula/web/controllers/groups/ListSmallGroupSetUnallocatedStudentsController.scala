package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{UnallocatedStudentsInformation, ListSmallGroupSetUnallocatedStudentsCommand}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value=Array("/groups/{smallGroupSet}/unallocatedstudentspopup"))
class ListSmallGroupSetUnallocatedStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable smallGroupSet: SmallGroupSet, user: CurrentUser) =
		ListSmallGroupSetUnallocatedStudentsCommand(smallGroupSet, user)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: Appliable[UnallocatedStudentsInformation]): Mav = {

		val info = command.apply()

		Mav("groups/students",
			"unallocated" -> true,
			"students" -> info.membersNotInGroups,
			"userUniId" -> user.universityId,
			"userIsMember" -> info.userIsMember,
			"studentsCanSeeTutorName" -> info.showTutors
		).noLayout()
	}
}

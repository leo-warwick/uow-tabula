package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.groups.ListSmallGroupStudentsCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value=Array("/groups/group/{group}/studentspopup"))
class ListSmallGroupStudentsController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable group: SmallGroup) =
		new ListSmallGroupStudentsCommand(group)

	@RequestMapping
	def ajaxList(@ModelAttribute("command") command: ListSmallGroupStudentsCommand, user: CurrentUser): Mav = {

		val students = command.apply()
		val userIsMember = students.exists(_.universityId == user.universityId)
		val showTutors = command.group.groupSet.studentsCanSeeTutorName

		Mav("groups/students",
			"students" -> students,
			"userUniId" -> user.universityId,
			"userIsMember" -> userIsMember,
			"studentsCanSeeTutorName" -> showTutors
		).noLayout()
	}

}

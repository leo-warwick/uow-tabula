package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.ListStudentsGroupsCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

/**
 * Displays the groups that the current user is a member of.
 */
@Controller
@RequestMapping(Array("/groups/student/{member}"))
class StudentGroupsController extends GroupsController {

	@ModelAttribute("command")
	def command(
		@PathVariable member:Member,
		user:CurrentUser,
		@RequestParam(value = "academicYear", required = false) academicYear: AcademicYear
	) =
		ListStudentsGroupsCommand(member, user, Option(academicYear))

	@RequestMapping(method = Array(POST, GET))
	def listGroups(@ModelAttribute("command") command: Appliable[GroupsViewModel.ViewModules]): Mav = {
		val data = command.apply()

		val title = {

			val smallGroups = data.moduleItems.flatMap(_.setItems.map(_.set))

			val formats = smallGroups.map(_.format.description).distinct
			val pluralisedFormats = formats.map {
				case s:String if s == Example.description => s + "es"
				case s:String => s + "s"
				case _ =>
			}
			pluralisedFormats.mkString(", ")
		}

		Mav("groups/students_groups",
			"title" -> title,
			"data" -> data
		).noLayoutIf(ajax)
	}

}

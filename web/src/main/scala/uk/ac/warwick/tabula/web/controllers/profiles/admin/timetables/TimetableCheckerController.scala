package uk.ac.warwick.tabula.web.controllers.profiles.admin.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.admin.timetables.TimetableCheckerCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value=Array("/profiles/admin/timetablechecker"))
class TimetableCheckerController extends ProfilesController {

	type TimetableCheckerCommand = Appliable[Unit]

	@ModelAttribute("command")
	def command(): TimetableCheckerCommand = TimetableCheckerCommand()

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("command") cmd: TimetableCheckerCommand):Mav = {
		Mav("profiles/admin/timetablechecker")
	}

	@RequestMapping(method=Array(POST))
	def submit(@ModelAttribute("command") command: TimetableCheckerCommand): Mav = {
		command.apply()
		Mav("profiles/admin/timetablechecker_results")
	}
}
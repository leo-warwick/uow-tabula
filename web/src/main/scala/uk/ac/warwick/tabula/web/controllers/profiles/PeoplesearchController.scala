package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView


@Controller
@RequestMapping(value = Array("/profiles/view/peoplesearch/{member}"))
class PeoplesearchController extends ProfilesController {

	@ModelAttribute("peoplesearchCommand") def command(@PathVariable member: Member , user: CurrentUser) =
		PeoplesearchCommand(member, user)

	@RequestMapping(produces = Array("application/json"))
	def render(@ModelAttribute("peoplesearchCommand") cmd: Appliable[Map[String, String]]): Mav = {
		Mav(new JSONView(cmd.apply()))
	}
}
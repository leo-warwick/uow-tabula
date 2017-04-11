package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.ViewUnconfirmedMeetingRecordsCommand
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}/unconfirmed"))
class ViewUnconfirmedMeetingRecordsController extends ProfilesController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		ViewUnconfirmedMeetingRecordsCommand(mandatory(department), mandatory(relationshipType))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[String, Int]],
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		Mav("profiles/relationships/unconfirmed",
			"tutorMap" -> cmd.apply()
		)
	}

}
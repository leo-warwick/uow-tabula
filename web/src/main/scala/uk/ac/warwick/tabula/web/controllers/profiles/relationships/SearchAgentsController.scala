package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.profiles.SearchJSONHelpers
import uk.ac.warwick.tabula.commands.profiles.{AbstractSearchProfilesCommandState, SearchAgentsCommand}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController


@Controller
class SearchAgentsController extends ProfilesController with SearchJSONHelpers {

	type SearchAgentsCommand = Appliable[Seq[Member]] with AbstractSearchProfilesCommandState

	override val formMav = null

	@ModelAttribute("searchAgentsCommand")
	def searchAgentsCommand = SearchAgentsCommand(user)

	@RequestMapping(value=Array("/profiles/relationships/agents/search.json"), params=Array("query"))
	def submitAgentSearchJSON(@Valid @ModelAttribute("searchAgentsCommand") cmd: SearchAgentsCommand, errors: Errors): Mav = {
		submitJson(cmd, errors)
	}

}

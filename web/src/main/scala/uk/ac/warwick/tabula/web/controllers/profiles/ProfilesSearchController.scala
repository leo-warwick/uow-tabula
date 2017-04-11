package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.profiles.SearchJSONHelpers
import uk.ac.warwick.tabula.commands.profiles.{AbstractSearchProfilesCommandState, SearchProfilesCommand}
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.stereotype.Controller
import javax.validation.Valid

import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.util.core.StringUtils

@Controller
class ProfilesSearchController extends ProfilesController with SearchJSONHelpers {

	type SearchProfilesCommand = Appliable[Seq[Member]] with AbstractSearchProfilesCommandState

	val formMav = Mav("profiles/search/", "displayOptionToSave" -> false)

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand: SearchProfilesCommand = SearchProfilesCommand(currentMember, user)

	@RequestMapping(value=Array("/profiles/search"), params=Array("!query"))
	def home = Redirect(Routes.home)

	@RequestMapping(value=Array("/profiles/search"), params=Array("query"))
	def submitSearch(@Valid @ModelAttribute("searchProfilesCommand") cmd: SearchProfilesCommand, errors: Errors): Mav = {
		if (!StringUtils.hasText(cmd.query)) home
		else submit(cmd, errors, "profiles/profile/search/results")
	}

	@RequestMapping(value=Array("/profiles/search.json"), params=Array("query"))
	def submitSearchJSON(@Valid @ModelAttribute("searchProfilesCommand") cmd: SearchProfilesCommand, errors: Errors): Mav = {
		submitJson(cmd, errors)
	}

}
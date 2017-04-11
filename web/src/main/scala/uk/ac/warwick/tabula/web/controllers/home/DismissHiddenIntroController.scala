package uk.ac.warwick.tabula.web.controllers.home

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.home.DismissHiddenIntroCommand
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(Array("/settings/showIntro/{settingHash}"))
class DismissHiddenIntroController extends TabulaHomepageController {

	validatesSelf[DismissHiddenIntroCommand]

	var userSettingsService: UserSettingsService = Wire.auto[UserSettingsService]

	private def getUserSettings(user: CurrentUser) =
		userSettingsService.getByUserId(user.apparentId)

	@ModelAttribute def command(user: CurrentUser, @PathVariable("settingHash") hash: String): DismissHiddenIntroCommand = {
		val usersettings = getUserSettings(user)
		usersettings match {
			case Some(setting) => new DismissHiddenIntroCommand(user, setting, hash)
			case None => new DismissHiddenIntroCommand(user, new UserSettings(user.apparentId), hash)
		}
	}


	@RequestMapping(method=Array(GET, HEAD))
	def viewSettings(user: CurrentUser, command: DismissHiddenIntroCommand, errors:Errors): Mav = {
		 Mav(new JSONView(Map("dismiss" -> command.dismiss)))
	}

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid command: DismissHiddenIntroCommand, errors:Errors): Mav = {
		if (errors.hasErrors){
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()
			Mav(new JSONView(Map("status" -> "success")))
		}
	}
}

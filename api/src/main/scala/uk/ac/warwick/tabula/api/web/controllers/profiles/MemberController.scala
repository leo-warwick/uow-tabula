package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.MemberToJsonConverter
import uk.ac.warwick.tabula.commands.profiles.profile.ViewProfileCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{AutowiringScalaFreemarkerConfigurationComponent, JSONView}

@Controller
@RequestMapping(Array("/v1/member/{member}"))
class MemberController extends ApiController
	with GetMemberApi
	with MemberToJsonConverter
	with AutowiringScalaFreemarkerConfigurationComponent

trait GetMemberApi {
	self: ApiController with MemberToJsonConverter =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable member: Member): ViewProfileCommand =
		new ViewProfileCommand(user, mandatory(member))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getMember(@ModelAttribute("getCommand") command: ViewProfileCommand): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"member" -> jsonMemberObject(mandatory(command.apply()))
		)))
	}
}

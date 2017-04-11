package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.sysadmin._
import org.springframework.web.bind.annotation
import javax.validation.Valid

import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiResponse
import uk.ac.warwick.tabula.web.controllers.sysadmin.LtiConformanceTesterGenerateController.LtiConformanceTesterPopulateFormCommand
import uk.ac.warwick.tabula.commands.sysadmin.LtiConformanceTesterPopulateFormCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/turnitinlti"))
class TurnitinLtiAdminController extends BaseSysadminController {

	@RequestMapping
	def home = Mav("sysadmin/turnitinlti/home")

}

@Controller
@RequestMapping(value = Array("/sysadmin/turnitinlti/submitassignment"))
class TurnitinLtiSubmitAssignmentController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("turnitinLtiSubmitAssignmentCommand")
	def turnitinLtiSubmitAssignmentCommand(user: CurrentUser) = TurnitinLtiSubmitAssignmentCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitinlti/submit-assignment")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("turnitinLtiSubmitAssignmentCommand") cmd: Appliable[TurnitinLtiResponse], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			val response: TurnitinLtiResponse = cmd.apply()
			Mav("sysadmin/turnitinlti/submit-assignment-done",
				"response" -> response)
		}
}

@Controller
@RequestMapping(value = Array("/sysadmin/turnitinlti/submitpaper"))
class TurnitinLtiSubmitPaperController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("turnitinLtiSubmitPaperCommand")
	def turnitinLtiSubmitPaperCommand(user: CurrentUser) = TurnitinLtiSubmitPaperCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitinlti/submit-paper")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("turnitinLtiSubmitPaperCommand") cmd: Appliable[TurnitinLtiResponse], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/turnitinlti")
		}
}

@Controller
@RequestMapping(value = Array("/sysadmin/turnitinlti/listendpoints"))
class TurnitinLtiListEndpointsController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("turnitinLtiListEndpointsCommand")
	def turnitinLtiListEndpointsCommand(user: CurrentUser) = TurnitinLtiListEndpointsCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitinlti/list-endpoints")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("turnitinLtiListEndpointsCommand") cmd: Appliable[TurnitinLtiResponse], errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			cmd.apply()
			Redirect("/sysadmin/turnitinlti")
		}
}

@Controller
@RequestMapping(value = Array("/sysadmin/turnitinlti/submissiondetails"))
class TurnitinLtiSubmissionDetailsController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("turnitinLtiSubmissionDetailsCommand")
	def turnitinLtiSubmissionDetailsCommand(user: CurrentUser) = TurnitinLtiSubmissionDetailsCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitinlti/submission-details")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("turnitinLtiSubmissionDetailsCommand") cmd: Appliable[TurnitinLtiResponse], errors: Errors): Mav =
	if (errors.hasErrors){
		form()
	} else {
		val response: TurnitinLtiResponse = cmd.apply()
		Mav("sysadmin/turnitinlti/submission-result",
			"response" -> response,
			"submission_info" -> response.submissionInfo)
	}

}

object LtiConformanceTesterGenerateController {
	type LtiConformanceTesterPopulateFormCommand = Appliable[Map[String, String]]
	with LtiConformanceTesterPopulateFormCommandState
}

@Controller
@RequestMapping(value = Array("/sysadmin/turnitinlti/conformancetester-generate"))
class LtiConformanceTesterGenerateController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("ltiConformanceTesterPopulateFormCommand")
	def ltiConformanceTesterPopulateFormCommand(user: CurrentUser) = LtiConformanceTesterPopulateFormCommand(user)

	@annotation.RequestMapping(method=Array(GET, HEAD))
	def form() = Mav("sysadmin/turnitinlti/conformance-tester-generate")

	@annotation.RequestMapping(method=Array(POST))
	def add(@Valid @ModelAttribute("ltiConformanceTesterPopulateFormCommand") cmd: LtiConformanceTesterPopulateFormCommand, errors: Errors): Mav =
		if (errors.hasErrors){
			form()
		} else {
			val response = cmd.apply()
			Mav("sysadmin/turnitinlti/conformance-tester-generate-result",
				"response" -> response,
				"endpoint" -> cmd.endpoint)
		}
}
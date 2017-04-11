package uk.ac.warwick.tabula.web.controllers.cm2

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{RequestExtensionCommand, RequestExtensionCommandState}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/assignment/{assignment}/extension"))
class ExtensionRequestController extends CourseworkController {

	type ExtensionRequestCommand = Appliable[Extension] with RequestExtensionCommandState

	var profileService: ProfileService = Wire.auto[ProfileService]

	@ModelAttribute("command")
	def cmd(
		@PathVariable assignment:Assignment,
		@RequestParam(defaultValue = "")
		action: String,
		user:CurrentUser
	) = RequestExtensionCommand(assignment, user, action)

	validatesSelf[SelfValidating]

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(@ModelAttribute("command") cmd: ExtensionRequestCommand): Mav = {

		val assignment = cmd.assignment

		if (!assignment.module.adminDepartment.allowExtensionRequests) {
			logger.info("Rejecting access to extension request screen as department does not allow extension requests")
			throw new PermissionDeniedException(user, Permissions.Extension.MakeRequest, assignment)
		} else {
			val existingRequest = assignment.findExtension(user.userId)
			existingRequest.foreach(cmd.presetValues)
			val profile = profileService.getMemberByUser(user.apparentUser)
			// is this an edit of an existing request
			val isModification = existingRequest.isDefined && !existingRequest.get.isManual
			Mav(s"$urlPrefix/submit/extension_request",
				"profile" -> profile,
				"module" -> assignment.module,
				"assignment" -> assignment,
				"department" -> assignment.module.adminDepartment,
				"isModification" -> isModification,
				"existingRequest" -> existingRequest.orNull,
				"command" -> cmd,
				"returnTo" -> getReturnTo(Routes.cm2.assignment(assignment))
			)

		}
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@Valid @ModelAttribute("command") cmd: ExtensionRequestCommand, errors: Errors): Mav = {
		if (errors.hasErrors){
			showForm(cmd)
		} else {
			val extension = cmd.apply()
			Mav(
				s"$urlPrefix/submit/extension_request_success",
				"isReply" -> extension.moreInfoReceived,
				"assignment" -> cmd.assignment
			)
		}
	}

}

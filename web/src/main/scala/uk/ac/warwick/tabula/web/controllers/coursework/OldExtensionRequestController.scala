package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.coursework.assignments.extensions._
import uk.ac.warwick.tabula.data.model.forms.Extension
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/extension"))
class OldExtensionRequestController extends OldCourseworkController{

	var profileService: ProfileService = Wire.auto[ProfileService]

	@ModelAttribute("command")
	def cmd(
		@PathVariable module: Module,
		@PathVariable assignment:Assignment,
		@RequestParam(defaultValue = "")
		action: String,
		user:CurrentUser
	) =
		RequestExtensionCommand(module, assignment, user, action)

	validatesSelf[SelfValidating]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module): Mav
	= mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(@ModelAttribute("command") cmd: Appliable[Extension] with RequestExtensionCommandState): Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)

		if (!module.adminDepartment.canRequestExtension) {
			logger.info("Rejecting access to extension request screen as department does not allow extension requests")
			throw new PermissionDeniedException(user, Permissions.Extension.MakeRequest, assignment)
		} else {
			if (user.loggedIn){
				val existingRequest = assignment.findExtension(user.userId)
				existingRequest.foreach(cmd.presetValues)
				val profile = profileService.getMemberByUser(user.apparentUser)
				// is this an edit of an existing request
				val isModification = existingRequest.isDefined && !existingRequest.get.isManual
				Mav(s"$urlPrefix/submit/extension_request",
					"profile" -> profile,
					"module" -> module,
					"assignment" -> assignment,
					"department" -> module.adminDepartment,
					"isModification" -> isModification,
					"existingRequest" -> existingRequest.orNull,
					"command" -> cmd,
					"returnTo" -> getReturnTo(s"/$urlPrefix" + Routes.assignment(assignment))
				)
			} else {
				RedirectToSignin()
			}
		}
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@Valid @ModelAttribute("command") cmd: Appliable[Extension] with RequestExtensionCommandState, errors: Errors): Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)

		if (errors.hasErrors){
			showForm(cmd)
		} else {
			val extension = cmd.apply()
			val model = Mav(s"$urlPrefix/submit/extension_request_success",
				"module" -> module,
				"assignment" -> assignment
			)
			crumbed(model, module)
		}
	}
}

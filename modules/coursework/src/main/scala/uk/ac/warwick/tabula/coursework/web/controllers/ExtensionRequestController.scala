package uk.ac.warwick.tabula.coursework.web.controllers

import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.ExtensionRequestCommand
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.hibernate.validator.Valid
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/extension"))
class ExtensionRequestController extends CourseworkController{

	@ModelAttribute
	def extensionRequestCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment:Assignment, user:CurrentUser) =
		new ExtensionRequestCommand(module, assignment, user)
	
	validatesSelf[ExtensionRequestCommand]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(cmd:ExtensionRequestCommand):Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)
		
		if (!module.department.canRequestExtension) {
			logger.info("Rejecting access to extension request screen as department does not allow extension requests")
			throw new PermissionDeniedException(user, Permissions.Extension.MakeRequest, assignment)
		} else {
			if (user.loggedIn){
				val existingRequest = assignment.findExtension(user.universityId)
				existingRequest.foreach(cmd.presetValues(_))
				// is this an edit of an existing request
				val isModification = existingRequest.isDefined && !existingRequest.get.isManual
				Mav("submit/extension_request",
					"module" -> module,
					"assignment" -> assignment,
					"isClosed" -> assignment.isClosed,
					"department" -> module.department,
					"isModification" -> isModification,
					"existingRequest" -> existingRequest.getOrElse(null),
					"command" -> cmd,
					"returnTo" -> getReturnTo("/coursework" + Routes.assignment(assignment))
				)
			} else {
				RedirectToSignin()
			}
		}
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@Valid cmd:ExtensionRequestCommand, errors: Errors):Mav = {
		val (assignment, module) = (cmd.assignment, cmd.module)
		
		if(errors.hasErrors){
			showForm(cmd)
		} else {
			val extension = cmd.apply()
			val model = Mav("submit/extension_request_success",
				"module" -> module,
				"assignment" -> assignment
			)
			crumbed(model, module)
		}
	}
}

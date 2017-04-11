package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/batch"))
class OldAddBatchFeedbackController extends OldCourseworkController {
	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(module, assignment, user.apparentUser, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, module: Module): Mav = mav.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))

	@RequestMapping(method = Array(HEAD, GET))
	def uploadZipForm(@ModelAttribute cmd: AddFeedbackCommand): Mav = {
		crumbed(Mav(s"$urlPrefix/admin/assignments/feedback/zipform"), cmd.module)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadZipForm(cmd)
		} else {
			cmd.postExtractValidation(errors)
			crumbed(Mav(s"$urlPrefix/admin/assignments/feedback/zipreview"), cmd.module)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@ModelAttribute cmd: AddFeedbackCommand, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)
		if (errors.hasErrors) {
			crumbed(Mav(s"$urlPrefix/admin/assignments/feedback/zipreview"), cmd.module)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module))
		}
	}
}
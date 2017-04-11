package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands.coursework.assignments.AddMarkerFeedbackCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback"))
class OldAddMarkerFeedbackController extends OldCourseworkController {

	@ModelAttribute def command(@PathVariable module: Module,
															@PathVariable assignment: Assignment,
															@PathVariable marker: User,
															user: CurrentUser) =
		new AddMarkerFeedbackCommand(module, assignment, marker, user)

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable module: Module,
								 @PathVariable assignment: Assignment,
								 @PathVariable marker: User,
								 @ModelAttribute cmd: AddMarkerFeedbackCommand): Mav = {
		Mav(s"$urlPrefix/admin/assignments/markerfeedback/form",
			"isProxying" -> cmd.isProxying,
			"proxyingAs" -> marker
		).crumbs(
			Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmUpload(@PathVariable module: Module,
										@PathVariable assignment: Assignment,
										@PathVariable marker: User,
										@ModelAttribute cmd: AddMarkerFeedbackCommand,
										errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadForm(module, assignment, marker, cmd)
		} else {
			cmd.postExtractValidation(errors)
			cmd.processStudents()
			Mav(s"$urlPrefix/admin/assignments/markerfeedback/preview",
				"isProxying" -> cmd.isProxying,
				"proxyingAs" -> marker
			).crumbs(
				Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable module: Module,
							 @PathVariable assignment: Assignment,
							 @PathVariable marker: User,
							 @ModelAttribute cmd: AddMarkerFeedbackCommand,
							 errors: Errors): Mav = {

		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)

		if (errors.hasErrors) {
			confirmUpload(module, assignment, marker, cmd, errors)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.assignment.markerFeedback(assignment, marker))
		}
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback"))
class OldAddMarkerFeedbackControllerCurrentUser extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.feedback(assignment, currentUser.apparentUser))
	}
}
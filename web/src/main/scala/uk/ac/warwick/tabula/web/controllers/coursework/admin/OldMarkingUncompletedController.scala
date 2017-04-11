package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.assignments.{CanProxy, MarkingUncompletedCommand, MarkingUncompletedState}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/marking-uncompleted"))
class OldMarkingUncompletedController extends OldCourseworkController {

	type MarkingUncompletedCommand = Appliable[Unit] with MarkingUncompletedState with CanProxy

	validatesSelf[SelfValidating]

	@ModelAttribute("markingUncompletedCommand")
	def command(@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							@PathVariable marker: User,
							submitter: CurrentUser) =
		MarkingUncompletedCommand(module, assignment, marker, submitter)

	def RedirectBack(assignment: Assignment, command: MarkingUncompletedCommand): Mav = {
			Redirect(Routes.admin.assignment.markerFeedback(assignment, command.user))
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand) = RedirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
		errors: Errors
	): Mav = {

		val previousStageRole = requestInfo
			.flatMap(_.requestParameters.get("previousStageRole"))
			.flatMap(_.headOption)

		Mav(s"$urlPrefix/admin/assignments/markerfeedback/marking-uncomplete",
			"assignment" -> assignment,
			"formAction" -> Routes.admin.assignment.markerFeedback.uncomplete(assignment, marker, previousStageRole.getOrElse("Marker")),
			"marker" -> form.user,
			"previousStageRole" -> previousStageRole,
			"isProxying" -> form.isProxying,
			"proxyingAs" -> marker
		).crumbs(
			Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@Valid @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
		errors: Errors
	): Mav = transactional() {
			if (errors.hasErrors)
				showForm(module,assignment, marker, form, errors)
			else {
				form.apply()
				RedirectBack(assignment, form)
			}
		}

}


// Redirects users trying to access a marking workflow using the old style URL
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/marking-uncompleted"))
class OldMarkingUncompletedControllerCurrentUser extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.uncomplete(assignment, currentUser.apparentUser))
	}

}
package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.assignments.{AdminMarkingUncompletedCommand, MarkingUncompletedState}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/return-submissions"))
class OldReturnForMarkingController extends OldCourseworkController {

	type MarkingUncompletedCommand = Appliable[Unit] with MarkingUncompletedState

	validatesSelf[SelfValidating]

	@ModelAttribute("markingUncompletedCommand")
	def command(@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							submitter: CurrentUser) =
		AdminMarkingUncompletedCommand(module, assignment, submitter.apparentUser, submitter)

	def redirectBack(assignment: Assignment, command: MarkingUncompletedCommand): Mav = {
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment, @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand): Mav = redirectBack(assignment, form)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
								@PathVariable module: Module,
								@PathVariable assignment: Assignment,
								@ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
								errors: Errors
								): Mav = {

		Mav(s"$urlPrefix/admin/assignments/markerfeedback/marking-uncomplete",
			"assignment" -> assignment,
			"formAction" -> Routes.admin.assignment.markerFeedback.returnsubmissions(assignment),
			"marker" -> form.user,
			"previousStageRole" -> "Marker"
		).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
							@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							@Valid @ModelAttribute("markingUncompletedCommand") form: MarkingUncompletedCommand,
							errors: Errors
							): Mav = transactional() {
		if (errors.hasErrors)
			showForm(module,assignment, form, errors)
		else {
			form.apply()
			redirectBack(assignment, form)
		}
	}

}
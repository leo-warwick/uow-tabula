package uk.ac.warwick.tabula.web.controllers.cm2.marker

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{MarkingCompletedCommand, MarkingCompletedState}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/{stage}/marking-completed"))
class MarkingCompletedController extends CourseworkController {

	validatesSelf[SelfValidating]

	type Command = Appliable[Seq[AssignmentFeedback]] with MarkingCompletedState

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, @PathVariable stage: MarkingWorkflowStage, currentUser: CurrentUser): Command =
		MarkingCompletedCommand(mandatory(assignment), mandatory(marker), currentUser, mandatory(stage))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@ModelAttribute("command") command: Command) =
		Redirect(Routes.admin.assignment.markerFeedback(command.assignment, command.marker))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@ModelAttribute("command") command: Command, errors: Errors): Mav = {
		Mav("cm2/admin/assignments/markers/marking_complete",
			"assignment" -> command.assignment,
			"department" -> command.assignment.module.adminDepartment,
			"stage" -> command.stage,
			"marker" -> command.marker,
			"isProxying" -> command.isProxying,
			"proxyingAs" -> command.marker
		)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@Valid @ModelAttribute("command") command: Command, errors: Errors): Mav = {
		if (errors.hasErrors)
			showForm(command, errors)
		else {
			transactional() {
				command.apply()
				Redirect(Routes.admin.assignment.markerFeedback(command.assignment, command.marker))
			}
		}
	}



}

package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.{StopMarkingMarkingCommand, StopMarkingRequest}
import uk.ac.warwick.tabula.commands.cm2.assignments.{ReleaseForMarkingCommand, ReleaseForMarkingRequest}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{AdminSelectionAction, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/stop-marking"))
class StopMarkingController extends CourseworkController with AdminSelectionAction {

	validatesSelf[SelfValidating]
	type Command = Appliable[Seq[AssignmentFeedback]] with StopMarkingRequest

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, user: CurrentUser): Command = {
		mandatory(Option(assignment.cm2MarkingWorkflow))
		StopMarkingMarkingCommand(mandatory(assignment), user)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable assignment: Assignment, @ModelAttribute("command") cmd: Command, errors: Errors): Mav = {
		Mav("cm2/admin/assignments/submissionsandfeedback/stop-marking")
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("command") cmd: Command, errors: Errors): Mav = {
		if (errors.hasErrors)
			showForm(assignment, cmd, errors)
		else {
			cmd.apply()
			RedirectBack(assignment)
		}
	}

}

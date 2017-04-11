package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.assignments.{BulkModerationApprovalCommand, BulkModerationApprovalState}
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/moderation/bulk-approve"))
class OldBulkModerationApprovalController extends OldCourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		submitter: CurrentUser
	) = BulkModerationApprovalCommand(mandatory(assignment), marker, submitter, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@ModelAttribute("command") command: Appliable[Unit], errors: Errors): Mav = {
		Mav(s"$urlPrefix/admin/assignments/markerfeedback/bulk-approve")
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
							@PathVariable assignment: Assignment,
							@ModelAttribute("command") @Valid command: Appliable[Unit] with BulkModerationApprovalState,
							errors: Errors): Mav =
	{
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Redirect(Routes.admin.assignment.markerFeedback(assignment, command.marker))
		}
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/online/moderation/bulk"))
class OldBulkModerationApprovalControllerCurrentUser extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.bulkApprove(assignment, currentUser.apparentUser))
	}
}
package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SelectedModerationModerator
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{AdminSelectionAction, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/moderator-sampling"))
class AllocateToModeratorsController extends CourseworkController with AdminSelectionAction {

	validatesSelf[SelfValidating]
	type ListCommand = Appliable[Seq[AllocateToModeratorInfo]] with AllocateModeratorsListState
	type ActionCommand = Appliable[Seq[Feedback]] with AllocateModeratorsActionState

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable assignment: Assignment, user: CurrentUser): ListCommand = {
		mandatory(Option(assignment.cm2MarkingWorkflow))
		AllocateModeratorsListCommand(mandatory(assignment), user.apparentUser)
	}

	@ModelAttribute("allocateCommand")
	def allocateCommand(@PathVariable assignment: Assignment, user: CurrentUser): ActionCommand = {
		mandatory(Option(assignment.cm2MarkingWorkflow))
		AllocateModeratorsCommand(mandatory(assignment), user.apparentUser)
	}

	@ModelAttribute("finaliseCommand")
	def finaliseCommand(@PathVariable assignment: Assignment, user: CurrentUser): ActionCommand = {
		mandatory(Option(assignment.cm2MarkingWorkflow))
		AdminFinaliseCommand(mandatory(assignment), user.apparentUser)
	}

	@RequestMapping(method = Array(POST))
	def showList(@PathVariable assignment: Assignment, @ModelAttribute("listCommand") list: ListCommand, errors: Errors): Mav =
		Mav("cm2/admin/assignments/submissionsandfeedback/allocate-moderators-list",
			"infos" -> list.apply(),
			"stage" -> SelectedModerationModerator
		).crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(value = Array("allocate", "finalise-feedback"))
	def actionGet(@PathVariable assignment: Assignment): Mav = RedirectBack(assignment)

	@RequestMapping(value = Array("allocate"), method = Array(POST), params = Array("!confirmScreen"))
	def allocatePreview(@PathVariable assignment: Assignment, @ModelAttribute("allocateCommand") cmd: ActionCommand, errors: Errors) : Mav =
		Mav("cm2/admin/assignments/submissionsandfeedback/allocate-moderators-allocate").crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(value = Array("allocate"), method = Array(POST), params = Array("confirmScreen"))
	def allocateSubmit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("allocateCommand") cmd: ActionCommand, errors: Errors) : Mav =
		if (errors.hasErrors)
			allocatePreview(assignment, cmd, errors)
		else {
			cmd.apply()
			RedirectBack(assignment)
		}

	@RequestMapping(value = Array("finalise-feedback"), method = Array(POST), params = Array("!confirmScreen"))
	def finalisePreview(@PathVariable assignment: Assignment, @ModelAttribute("finaliseCommand") cmd: ActionCommand, errors: Errors) : Mav =
		Mav("cm2/admin/assignments/submissionsandfeedback/allocate-moderators-finalise").crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(value = Array("finalise-feedback"), method = Array(POST), params = Array("confirmScreen"))
	def finaliseSubmit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("finaliseCommand") cmd: ActionCommand, errors: Errors): Mav =
		if (errors.hasErrors)
			finalisePreview(assignment, cmd, errors)
		else {
			cmd.apply()
			RedirectBack(assignment)
		}


}

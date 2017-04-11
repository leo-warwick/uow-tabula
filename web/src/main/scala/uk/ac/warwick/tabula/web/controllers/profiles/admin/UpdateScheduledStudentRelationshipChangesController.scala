package uk.ac.warwick.tabula.web.controllers.profiles.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.{BindException, BindingResult}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.profiles.relationships.{ApplyScheduledStudentRelationshipChangesCommand, CancelScheduledStudentRelationshipChangesCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

object UpdateScheduledStudentRelationshipChangesControllerActions {
	final val Apply = "apply"
	final val Cancel = "cancel"
}

@Controller
@RequestMapping(Array("/profiles/department/{department}/{relationshipType}/scheduled/update"))
class UpdateScheduledStudentRelationshipChangesController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam action: String
	): Appliable[Seq[StudentRelationship]] = action match {
		case UpdateScheduledStudentRelationshipChangesControllerActions.Apply =>
			ApplyScheduledStudentRelationshipChangesCommand(mandatory(relationshipType), mandatory(department), user)
		case UpdateScheduledStudentRelationshipChangesControllerActions.Cancel =>
			CancelScheduledStudentRelationshipChangesCommand(mandatory(relationshipType), mandatory(department), user)
		case _ =>
			throw new IllegalArgumentException
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]],
		errors: BindingResult,
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		if (errors.hasErrors) {
			throw new BindException(errors)
		} else {
			cmd.apply()
			Redirect(Routes.relationships.scheduled(department, relationshipType))
		}
	}

}

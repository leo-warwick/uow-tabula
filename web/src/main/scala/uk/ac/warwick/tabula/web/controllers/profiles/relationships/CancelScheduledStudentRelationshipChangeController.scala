package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.{BindException, BindingResult}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships.CancelScheduledStudentRelationshipChangesCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

object CancelScheduledStudentRelationshipChangeController {
	final val scheduledAgentChangeCancel = "scheduledAgentChangeCancel"
}

@Controller
@RequestMapping(Array("/profiles/{relationshipType}/{studentCourseDetails}/cancel/{relationship}"))
class CancelScheduledStudentRelationshipChangeController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable relationship: StudentRelationship): Appliable[Seq[StudentRelationship]] =
		CancelScheduledStudentRelationshipChangesCommand.applyForRelationship(
			mandatory(relationship).relationshipType,
			mandatory(relationship).studentCourseDetails.department,
			user,
			mandatory(relationship)
		)

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]], errors: BindingResult): Mav = {
		if (errors.hasErrors) {
			throw new BindException(errors)
		} else {
			val result = cmd.apply()
			Redirect(Routes.Profile.relationshipType(result.head.studentCourseDetails.student, result.head.relationshipType),
				"scheduledAgentChangeCancel" -> true
			)
		}
	}

}

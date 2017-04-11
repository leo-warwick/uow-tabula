package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.profiles.relationships.{EditStudentRelationshipCommand, EditStudentRelationshipCommandRequest}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

object ManageStudentRelationshipController {
	final val scheduledAgentChange = "scheduledAgentChange"
}

trait ManageStudentRelationshipController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @PathVariable studentCourseDetails: StudentCourseDetails) =
		EditStudentRelationshipCommand(studentCourseDetails, relationshipType, user)

	def render(agent: Option[Member]): Mav = {
		Mav("profiles/relationships/edit/view", "existingAgent" -> agent).noLayoutIf(ajax)
	}

	def redirectResult(
		relationshipType: StudentRelationshipType,
		studentCourseDetails: StudentCourseDetails,
		scheduledDate: DateTime
	): Mav = {
		val r = Redirect(Routes.Profile.relationshipType(studentCourseDetails.student, relationshipType))
		if (scheduledDate.isAfterNow) {
			r.addObjects(
				ManageStudentRelationshipController.scheduledAgentChange ->
					DateTimeFormat.forPattern(DateFormats.DateTimePickerPattern).print(scheduledDate)
			)
		}
		r
	}

}

@Controller
@RequestMapping(Array("/profiles/{relationshipType}/{studentCourseDetails}/add"))
class AddStudentRelationshipController extends ManageStudentRelationshipController {

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]]): Mav = {
		render(None)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]] with EditStudentRelationshipCommandRequest,
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails
	): Mav = {
		if (errors.hasErrors){
			form(cmd)
		} else {
			cmd.apply()

			redirectResult(relationshipType, studentCourseDetails, cmd.scheduledDateToUse)
		}
	}

}

@Controller
@RequestMapping(Array("/profiles/{relationshipType}/{studentCourseDetails}/edit/{agent}"))
class EditStudentRelationshipController extends ManageStudentRelationshipController {

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]],
		@PathVariable("agent") agent: Member
	): Mav = {
		render(Some(agent))
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]] with EditStudentRelationshipCommandRequest,
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable("agent") agent: Member
	): Mav = {
		if (errors.hasErrors){
			form(cmd, agent)
		} else {
			cmd.apply()

			redirectResult(relationshipType, studentCourseDetails, cmd.scheduledDateToUse)
		}
	}

}

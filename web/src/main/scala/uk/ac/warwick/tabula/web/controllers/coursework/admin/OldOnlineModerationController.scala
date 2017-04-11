package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateGradesFromMarkCommand, OnlineModerationCommand}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/online/moderation/{student}"))
class OldOnlineModerationController extends OldCourseworkController {

	validatesSelf[OnlineModerationCommand]

	@ModelAttribute("command")
	def command(
		@PathVariable student: User,
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		submitter: CurrentUser
	) = OnlineModerationCommand(module, assignment, student, marker, submitter, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineModerationCommand, errors: Errors): Mav = {

		val (isCompleted, completedDate, firstMarkerFeedback) = command.markerFeedback match {
			case Some(mf) => (
				mf.state == MarkingCompleted || mf.state == Rejected ,
				mf.uploadedDate,
				mf.feedback.firstMarkerFeedback
			)
			case None => (false, null, None)
		}

		Mav(s"$urlPrefix/admin/assignments/feedback/marker_moderation" ,
			"command" -> command,
			"isCompleted" -> isCompleted,
			"completedDate" -> completedDate,
			"firstMarkerFeedback" -> firstMarkerFeedback,
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineModerationCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			val markerFeedbackState: String = command.markerFeedback.map(_.state.toString).getOrElse("")
			Mav("ajax_success", "data" -> markerFeedbackState).noLayout()
		}
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/online/moderation/{student}"))
class OldOnlineModerationControllerCurrentUser extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, @PathVariable student: User, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.onlineFeedback.moderation(assignment, currentUser.apparentUser, student))
	}
}

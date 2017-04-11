package uk.ac.warwick.tabula.web.controllers.coursework.admin
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.feedback._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.commands.coursework.feedback.PublishFeedbackCommand.PublishFeedbackResults
import uk.ac.warwick.tabula.services.{AutowiringFeedbackForSitsServiceComponent, AutowiringFeedbackServiceComponent}

@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/publish"))
@Profile(Array("cm1Enabled")) @Controller
class OldPublishFeedbackController extends OldCourseworkController {

	type PublishFeedbackCommand = Appliable[PublishFeedbackCommand.PublishFeedbackResults] with PublishFeedbackCommandState
	validatesSelf[SelfValidating]

	@ModelAttribute("publishFeedbackCommand") def cmd(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): PublishFeedbackCommandInternal with ComposableCommand[PublishFeedbackResults] with AutowiringFeedbackServiceComponent with AutowiringFeedbackForSitsServiceComponent with PublishFeedbackCommandState with PublishFeedbackPermissions with PublishFeedbackValidation with PublishFeedbackDescription with PublishFeedbackNotification with PublishFeedbackNotificationCompletion with QueuesFeedbackForSits = {
		PublishFeedbackCommand(mandatory(module), mandatory(assignment), user, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!confirm"))
	def confirmation(@ModelAttribute("publishFeedbackCommand") command: PublishFeedbackCommand, errors: Errors): Mav = {
		if (errors.hasErrors) command.prevalidate(errors)
		Mav(s"$urlPrefix/admin/assignments/publish/form",
			"assignment" -> command.assignment,
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation,
			"gradeValidation" -> command.validateGrades
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("publishFeedbackCommand") command: PublishFeedbackCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			confirmation(command, errors)
		} else {
			command.apply()
			Mav(s"$urlPrefix/admin/assignments/publish/done", "assignment" -> command.assignment)
		}
	}

}
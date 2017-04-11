package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateGradesFromMarkCommand, OnlineFeedbackCommand, OnlineFeedbackFormCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/online"))
class OldOnlineFeedbackController extends OldCourseworkController with AutowiringUserLookupComponent {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, submitter: CurrentUser) =
		OnlineFeedbackCommand(mandatory(module), mandatory(assignment), submitter)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		Mav(s"$urlPrefix/admin/assignments/feedback/online_framework",
			"showMarkingCompleted" -> false,
			"showGenericFeedback" -> true,
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs,
			"onlineMarkingUrls" -> feedbackGraphs.map{ graph =>
				graph.student.getUserId -> Routes.admin.assignment.onlineFeedback(assignment)
			}.toMap
		).crumbs(
				Breadcrumbs.Department(module.adminDepartment),
				Breadcrumbs.Module(module)
			)
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OldOnlineFeedbackFormController extends OldCourseworkController {

	validatesSelf[OnlineFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineFeedbackFormCommand(module, assignment, student, currentUser.apparentUser, currentUser, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineFeedbackFormCommand, errors: Errors): Mav = {

		Mav(s"$urlPrefix/admin/assignments/feedback/online_feedback",
			"command" -> command,
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineFeedbackFormCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}

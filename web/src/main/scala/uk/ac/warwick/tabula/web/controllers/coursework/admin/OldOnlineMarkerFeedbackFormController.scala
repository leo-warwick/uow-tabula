package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, MarkingMethod, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.commands.coursework.feedback.{GenerateGradesFromMarkCommand, OnlineMarkerFeedbackFormCommand}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/online/{student}"))
class OldOnlineMarkerFeedbackFormController extends OldCourseworkController {

	validatesSelf[OnlineMarkerFeedbackFormCommand]

	@ModelAttribute("command")
	def command(
		@PathVariable student: User,
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		submitter: CurrentUser
	) =	OnlineMarkerFeedbackFormCommand(
		mandatory(module),
		mandatory(assignment),
		student,
		marker,
		submitter,
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))
	)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {

		val isCompleted = command.allMarkerFeedbacks.forall(_.state == MarkingCompleted)

		val parentFeedback = command.allMarkerFeedbacks.head.feedback
		val isRejected = command.allMarkerFeedbacks.exists(_.state == Rejected)
		val isCurrentUserFeebackEntry = parentFeedback.getCurrentWorkflowFeedback.exists(_.getMarkerUser.exists { _ == command.marker })
		val allCompletedMarkerFeedback = parentFeedback.getAllCompletedMarkerFeedback


		Mav(s"$urlPrefix/admin/assignments/feedback/marker_online_feedback" ,
			"command" -> command,
			"isCompleted" -> isCompleted,
			"isRejected" -> isRejected,
			"allCompletedMarkerFeedback" -> allCompletedMarkerFeedback,
			"isCurrentUserFeedbackEntry" -> isCurrentUserFeebackEntry,
			"parentFeedback" -> parentFeedback,
			"secondMarkerNotes" -> Option(parentFeedback.secondMarkerFeedback).map(_.rejectionComments).orNull,
			"isModerated" -> (command.assignment.markingWorkflow.markingMethod == MarkingMethod.ModeratedMarking),
			"isFinalMarking" -> Option(parentFeedback.secondMarkerFeedback).exists(_.state == MarkingCompleted),
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/online/{student}"))
class OldOnlineMarkerFeedbackFormControllerCurrentUser extends OldCourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, @PathVariable student: User, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.onlineFeedback.student(assignment, currentUser.apparentUser, student))
	}
}

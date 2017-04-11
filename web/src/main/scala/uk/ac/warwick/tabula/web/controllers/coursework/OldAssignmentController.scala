package uk.ac.warwick.tabula.web.controllers.coursework

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.coursework.assignments.{SubmitAssignmentCommand, SubmitAssignmentRequest}
import uk.ac.warwick.tabula.commands.coursework.{CurrentUserSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser}
/**
 * This is the main student-facing and non-student-facing controller for handling esubmission and return of feedback.
 * If the studentMember is not specified it works for the current user, whether they are a member of not.
 */

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}"))
class OldAssignmentController extends OldCourseworkController
	with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent with AutowiringFeaturesComponent {

	type StudentSubmissionAndFeedbackCommand = Appliable[StudentSubmissionInformation] with CurrentUserSubmissionAndFeedbackCommandState
	type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest

	hideDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("submitAssignmentCommand") def formOrNull(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): SubmitAssignmentCommand = {
		restricted(SubmitAssignmentCommand.self(mandatory(module), mandatory(assignment), user)).orNull
	}

	@ModelAttribute("studentSubmissionAndFeedbackCommand") def studentSubmissionAndFeedbackCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) =
		StudentSubmissionAndFeedbackCommand(module, assignment, user)

	@ModelAttribute("willCheckpointBeCreated")
	def willCheckpointBeCreated(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		user: CurrentUser
	): Boolean = {
		val submission = new Submission
		submission.assignment = assignment
		submission.submittedDate = DateTime.now
		submission._universityId = user.universityId
		submission.usercode = user.userId
		attendanceMonitoringCourseworkSubmissionService.getCheckpoints(submission).nonEmpty
	}

	/**
	 * Sitebuilder-embeddable view.
	 */
	@RequestMapping(method = Array(HEAD, GET), params = Array("embedded"))
	def embeddedView(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
			errors: Errors): Mav = {
		view(infoCommand, formOrNull, errors).embedded
	}

	@RequestMapping(method = Array(HEAD, GET), params = Array("!embedded"))
	def view(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@ModelAttribute("submitAssignmentCommand") formOrNull: SubmitAssignmentCommand,
			errors: Errors): Mav = {
		val form = Option(formOrNull)
		val info = infoCommand.apply()

		// If the user has feedback but doesn't have permission to submit, form will be null here, so we can't just get module/assignment from that
		Mav(
			s"$urlPrefix/submit/assignment",
			"errors" -> errors,
			"feedback" -> info.feedback,
			"submission" -> info.submission,
			"justSubmitted" -> form.exists { _.justSubmitted },
			"canSubmit" -> info.canSubmit,
			"canReSubmit" -> info.canReSubmit,
			"hasExtension" -> info.extension.isDefined,
			"hasActiveExtension" -> info.extension.exists(_.approved), // active = has been approved
			"extension" -> info.extension,
			"isExtended" -> info.isExtended,
			"extensionRequested" -> info.extensionRequested,
			"hasDisability" -> info.hasDisability,
			"isSelf" -> true)
			.withTitle(infoCommand.module.name + " (" + infoCommand.module.code.toUpperCase + ")" + " - " + infoCommand.assignment.name)
	}

	@RequestMapping(method = Array(POST))
	def submit(
			@ModelAttribute("studentSubmissionAndFeedbackCommand") infoCommand: StudentSubmissionAndFeedbackCommand,
			@Valid @ModelAttribute("submitAssignmentCommand") form: SubmitAssignmentCommand,
			errors: Errors): Mav = {
		// We know form isn't null here because of permissions checks on the info command
		if (errors.hasErrors) {
			view(infoCommand, form, errors)
		} else {
			transactional() { form.apply() }

			Redirect(Routes.assignment(form.assignment)).addObjects("justSubmitted" -> true)
		}
	}

}

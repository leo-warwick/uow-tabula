package uk.ac.warwick.tabula.web.controllers.coursework.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.feedback._
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/adjustments"))
class OldFeedbackAdjustmentsListController extends OldCourseworkController {

	type FeedbackAdjustmentListCommand = Appliable[Seq[StudentInfo]]

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable assignment: Assignment): FeedbackAdjustmentListCommand =
		FeedbackAdjustmentListCommand(mandatory(assignment))

	@RequestMapping
	def list(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listCommand") listCommand: FeedbackAdjustmentListCommand
	): Mav = {
		val (studentInfo, noFeedbackStudentInfo) = listCommand.apply().partition { _.feedback.isDefined }

		Mav(s"$urlPrefix/admin/assignments/feedback/adjustments_list",
			"studentInfo" -> studentInfo,
			"noFeedbackStudentInfo" -> noFeedbackStudentInfo,
			"assignment" -> assignment,
			"isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation
		).crumbs(
			Breadcrumbs.Department(assignment.module.adminDepartment),
			Breadcrumbs.Module(assignment.module)
		)
	}
}

object OldFeedbackAdjustmentsController {
	// TAB-3312 Source: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/faqs/penalties
	final val LatePenaltyPerDay: Map[CourseType, Int] = Map(
		CourseType.UG -> 5,
		CourseType.PGR -> 3,
		CourseType.PGT -> 3
	).withDefault { courseType => 5 }
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/adjustments/{student}"))
class OldFeedbackAdjustmentsController extends OldCourseworkController with AutowiringProfileServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def formCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable student: User, submitter: CurrentUser) =
		AssignmentFeedbackAdjustmentCommand(mandatory(assignment), student, submitter, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	@RequestMapping(method=Array(GET))
	def showForm(
		@ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState,
		@PathVariable assignment: Assignment,
		@PathVariable student: User
	): Mav = {
		val submission = assignment.findSubmission(student.getUserId)
		val daysLate = submission.map { _.workingDaysLate }

		val courseType = submission.flatMap { submission =>
				submission.universityId
					.flatMap(uid => profileService.getMemberByUniversityId(uid))
					.collect { case stu: StudentMember => stu }
					.flatMap { _.mostSignificantCourseDetails }
					.flatMap { _.courseType }
		}

		// Treat any unknowns as an undergraduate
		val latePenaltyPerDay = OldFeedbackAdjustmentsController.LatePenaltyPerDay(courseType.getOrElse(CourseType.UG))
		val marksSubtracted = daysLate.map(latePenaltyPerDay * _)

		val proposedAdjustment = {
			if (assignment.openEnded) None
			else {
				for(am <- command.feedback.actualMark; ms <- marksSubtracted)
				yield Math.max(0, am - ms)
			}
		}

		Mav(s"$urlPrefix/admin/assignments/feedback/adjustments", Map(
			"daysLate" -> daysLate,
			"marksSubtracted" -> marksSubtracted,
			"proposedAdjustment" -> proposedAdjustment,
			"latePenalty" -> latePenaltyPerDay,
			"isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation
		)).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState,
		errors: Errors,
		@PathVariable assignment: Assignment,
		@PathVariable student: User
	): Mav = {
		if (errors.hasErrors) {
			showForm(command, assignment, student)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}
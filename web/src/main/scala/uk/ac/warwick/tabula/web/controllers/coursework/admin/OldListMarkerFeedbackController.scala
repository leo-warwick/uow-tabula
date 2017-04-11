package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{CanProxy, ListMarkerFeedbackCommand, MarkerFeedbackStage}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/list"))
class OldListMarkerFeedbackController extends OldCourseworkController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable assignment: Assignment,
		@PathVariable module: Module,
		@PathVariable marker: User,
		submitter: CurrentUser
	) = ListMarkerFeedbackCommand(assignment, module, marker, submitter)

	@RequestMapping(method = Array(HEAD, GET))
	def list(
		@ModelAttribute("command") command: Appliable[Seq[MarkerFeedbackStage]] with CanProxy,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User
	): Mav = {
		if(assignment.markingWorkflow == null) {
			Mav(s"$urlPrefix/errors/no_workflow", "assignmentUrl" -> Routes.admin.assignment.submissionsandfeedback.summary(assignment))
		} else {
			val markerFeedback = command.apply()
			val feedbackItems = markerFeedback.flatMap(_.feedbackItems)
			val unsubmittedStudents = feedbackItems.filter(_.submission.isEmpty).map(_.student)
			val feedbackCounts: Seq[Int] = feedbackItems.map(_.feedbacks.size)
			val maxFeedbackCount = feedbackCounts.foldLeft(0)(_ max _)
			val hasFirstMarkerFeedback = maxFeedbackCount > 1
			val hasSecondMarkerFeedback = maxFeedbackCount > 2

			Mav(s"$urlPrefix/admin/assignments/markerfeedback/list",
				"assignment" -> assignment,
				"markerFeedback" -> markerFeedback,
				"feedbackToDoCount" -> markerFeedback.map(_.feedbackItems.size).sum,
				"hasFirstMarkerFeedback" -> hasFirstMarkerFeedback,
				"hasSecondMarkerFeedback" -> hasSecondMarkerFeedback,
				"firstMarkerRoleName" -> assignment.markingWorkflow.firstMarkerRoleName,
				"secondMarkerRoleName" -> assignment.markingWorkflow.secondMarkerRoleName,
				"thirdMarkerRoleName" -> assignment.markingWorkflow.thirdMarkerRoleName,
				"onlineMarkingUrls" -> feedbackItems.map{ items =>
					items.student.getUserId -> assignment.markingWorkflow.courseworkMarkingUrl(assignment, marker, items.student.getUserId)
				}.toMap,
				"marker" -> marker,
				"unsubmittedStudents" -> unsubmittedStudents,
				"isProxying" -> command.isProxying,
				"proxyingAs" -> marker
			)
		}
	}

}

// Redirects users trying to access a marking workflow using the old style URL
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/list"))
class OldListCurrentUsersMarkerFeedbackController extends OldCourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback(assignment, currentUser.apparentUser))
	}
}

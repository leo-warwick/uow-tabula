package uk.ac.warwick.tabula.web.controllers.cm2.marker

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.ListMarkerFeedbackCommand.EnhancedFeedbackForOrderAndStage
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{ListMarkerFeedbackCommand, ListMarkerFeedbackState}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentAnonymity}
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters.SubmissionStates._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters.PlagiarismStatuses._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters.Statuses._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

object ListMarkerFeedbackController {
	val AllPlagiarismFilters = Seq(NotCheckedForPlagiarism, CheckedForPlagiarism, MarkedPlagiarised)
	val AllSubmissionFilters = Seq(Submitted, Unsubmitted, OnTime, WithExtension, LateSubmission, ExtensionRequested, ExtensionDenied, ExtensionGranted)
	val AllMarkerStatuses = Seq(MarkedByMarker, NotMarkedByMarker, FeedbackByMarker, NoFeedbackByMarker, NotSentByMarker)
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}"))
class ListMarkerFeedbackController extends CourseworkController {

	type Command = Appliable[Seq[EnhancedFeedbackForOrderAndStage]] with ListMarkerFeedbackState
	import ListMarkerFeedbackController._

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, currentUser: CurrentUser): Command =
		ListMarkerFeedbackCommand(assignment, marker, currentUser)

	@RequestMapping
	def list(@PathVariable assignment: Assignment, @PathVariable marker: User, @ModelAttribute("command") command: Command): Mav = {
		val workflow = mandatory(command.assignment.cm2MarkingWorkflow)
		if (ajax) {
			Mav("cm2/admin/assignments/markers/marker_feedback_list",
				"feedbackByOrderAndStage" -> command.apply(),
				"assignment" -> command.assignment,
				"workflowType" -> workflow.workflowType,
				"marker" -> command.marker,
				"AssignmentAnonymity" -> AssignmentAnonymity
			).noLayout()
		} else {
			Mav("cm2/admin/assignments/markers/assignment",
				"allSubmissionStatesFilters" -> AllSubmissionFilters.filter(_.apply(command.assignment)),
				"allPlagiarismFilters" -> AllPlagiarismFilters.filter(_.apply(command.assignment)),
				"allMarkerStateFilters" -> AllMarkerStatuses.filter(_.apply(command.assignment)),
				"department" -> command.assignment.module.adminDepartment,
				"assignment" -> command.assignment,
				"isProxying" -> command.isProxying,
				"proxyingAs" -> marker
			).crumbsList(Breadcrumbs.markerAssignment(assignment, marker, active = true, proxying = command.isProxying))
		}
	}

}

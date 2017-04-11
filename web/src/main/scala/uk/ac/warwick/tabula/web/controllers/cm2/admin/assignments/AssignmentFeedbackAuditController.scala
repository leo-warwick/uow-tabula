package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignmentFeedbackAuditCommand, AssignmentFeedbackAuditResults}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkingMethod}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.web.{Breadcrumbs, Mav}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/audit/assignment/{assignment}"))
class AssignmentFeedbackAuditController extends CourseworkController {

	@ModelAttribute("auditCommand")
	def listCommand(@PathVariable assignment: Assignment) =
		AssignmentFeedbackAuditCommand(mandatory(assignment))


	@RequestMapping(method=Array(GET))
	def list(@PathVariable assignment: Assignment,
		@ModelAttribute("auditCommand") auditCommand: Appliable[AssignmentFeedbackAuditResults]
	): Mav = {
		val auditData = auditCommand.apply()
		Mav("cm2/admin/assignments/submissions_audit", "command" -> auditCommand,
			"auditData" -> auditData,
			"assignment" -> assignment,
			"isModerated" -> Option(assignment.markingWorkflow).exists(_.markingMethod == MarkingMethod.ModeratedMarking),
			"releasedFeedback" -> assignment.countReleasedFeedback)
			.secondCrumbs(Breadcrumbs.Standard("Audit", Some(Routes.admin.assignment.audit(assignment)), ""))
	}
}



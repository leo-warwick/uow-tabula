package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{FeedbackAuditCommand, FeedbackAuditData}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, MarkingMethod}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/audit/{student}"))
class OldFeedbackAuditController extends OldCourseworkController {

	@ModelAttribute("auditCommand")
	def listCommand(@PathVariable assignment: Assignment, @PathVariable student: User) =
		FeedbackAuditCommand(assignment, student)

	@RequestMapping(method=Array(GET))
	def list(@PathVariable assignment: Assignment,
					 @PathVariable student: User,
					 @ModelAttribute("auditCommand") auditCommand: Appliable[FeedbackAuditData]
						): Mav = {
		val auditData = auditCommand.apply()
		Mav(s"$urlPrefix/admin/assignments/feedback_audit",
			"command" -> auditCommand,
			"auditData" -> auditData,
			"assignment" -> assignment,
			"isModerated" -> Option(assignment.markingWorkflow).exists(_.markingMethod == MarkingMethod.ModeratedMarking),
			"student" -> student
		).crumbs(
				Breadcrumbs.Department(assignment.module.adminDepartment),
				Breadcrumbs.Module(assignment.module)
			)
	}
}
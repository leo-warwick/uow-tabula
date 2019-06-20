package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{FeedbackAuditCommand, FeedbackAuditData}
import uk.ac.warwick.tabula.data.model.{Assignment, MarkingMethod}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/audit/{student}"))
class FeedbackAuditController extends CourseworkController {

  @ModelAttribute("auditCommand")
  def listCommand(@PathVariable assignment: Assignment, @PathVariable student: User) =
    FeedbackAuditCommand(assignment, student)

  @RequestMapping(method = Array(GET))
  def list(@PathVariable assignment: Assignment,
    @PathVariable student: User,
    @ModelAttribute("auditCommand") auditCommand: Appliable[FeedbackAuditData]
  ): Mav = {
    val auditData = auditCommand.apply()
    Mav("cm2/admin/assignments/feedback_audit",
      "command" -> auditCommand,
      "auditData" -> auditData,
      "assignment" -> assignment,
      "isModerated" -> Option(assignment.markingWorkflow).exists(_.markingMethod == MarkingMethod.ModeratedMarking), // only needed for CM1 macros
      "student" -> student
    ).crumbsList(Breadcrumbs.assignment(assignment))

  }
}
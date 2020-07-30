package uk.ac.warwick.tabula.api.web.controllers.exams

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.commands.exams.AssessmentGroupAssignmentsCommand
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, AssignmentToJsonConverter, UpstreamAssessmentsAndExamsToJsonConverter}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView


@Controller
@RequestMapping(Array("/v1/department/{department}/assessmentGroupAssignments"))
class AssessmentGroupAssignmentsController extends ApiController
  with AssignmentToJsonConverter
  with AssessmentMembershipInfoToJsonConverter
  with UpstreamAssessmentsAndExamsToJsonConverter {

  @ModelAttribute("command")
  def command(@PathVariable department: Department): AssessmentGroupAssignmentsCommand.Command =
    AssessmentGroupAssignmentsCommand(mandatory(department))


  @RequestMapping(method = Array(GET, POST), produces = Array("application/json"))
  def assessmentGroupAssignments(
    @ModelAttribute("command") command: AssessmentGroupAssignmentsCommand.Command
  ): Mav = {
    val assignmentsByAssessmentGroup = command.apply()

    val assessmentGroups = assignmentsByAssessmentGroup.map{ case (ag, a) =>
      val assignments: (String, Any) = "assignments" -> a.map(jsonAssignmentObject(_))
      jsonUpstreamAssessmentGroupKeyObject(ag) + assignments
    }

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "assessmentGroups" -> assessmentGroups,
    )))
  }

}

package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.controllers.cm2.AbstractGenerateGradeFromMarkController

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/generate-grade"))
class GenerateAssignmentGradeFromMarkController extends AbstractGenerateGradeFromMarkController[Assignment] {

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment) =
    GenerateGradesFromMarkCommand(mandatory(assignment))

}

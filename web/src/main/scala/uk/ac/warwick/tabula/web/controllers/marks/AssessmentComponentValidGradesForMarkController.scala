package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.AssessmentComponentValidGradesForMarkCommand
import uk.ac.warwick.tabula.data.model.AssessmentComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/marks/admin/assessment-component/{assessmentComponent}/generate-grades"))
class AssessmentComponentValidGradesForMarkController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable assessmentComponent: AssessmentComponent): AssessmentComponentValidGradesForMarkCommand.Command =
    AssessmentComponentValidGradesForMarkCommand(assessmentComponent)

  @PostMapping
  def validGrades(@Valid @ModelAttribute("command") command: AssessmentComponentValidGradesForMarkCommand.Command, errors: Errors): Mav = {
    if (errors.hasErrors) {
      Mav("_generatedGrades",
        "grades" -> Seq(),
        "default" -> null
      ).noLayout()
    } else {
      val (grades, default) = command.apply()
      Mav("_generatedGrades",
        "grades" -> grades,
        "default" -> default
      ).noLayout()
    }
  }

}

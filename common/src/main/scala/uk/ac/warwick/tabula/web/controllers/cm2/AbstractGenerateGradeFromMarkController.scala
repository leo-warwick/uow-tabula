package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.feedback.{GenerateGradesFromMarkCommandRequest, GenerateGradesFromMarkCommandState}
import uk.ac.warwick.tabula.data.model.{Assessment, GradeBoundary}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.AbstractGenerateGradeFromMarkController.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.web.controllers.BaseController

import scala.jdk.CollectionConverters._

object AbstractGenerateGradeFromMarkController {
  type GenerateGradesFromMarkCommand = Appliable[Map[String, Seq[GradeBoundary]]] with GenerateGradesFromMarkCommandRequest with GenerateGradesFromMarkCommandState
}

abstract class AbstractGenerateGradeFromMarkController[A <: Assessment] extends BaseController {

  def command(assessment: A): GenerateGradesFromMarkCommand

  def defaultGrade(
    universityId: String,
    marks: Map[String, String],
    grades: Map[String, Seq[GradeBoundary]],
    selectedGrades: Map[String, String],
    useDefaultGradeForZero: Boolean
  ): Option[GradeBoundary] = {
    val mark = marks(universityId)
    if (selectedGrades.get(universityId).flatMap(_.maybeText).nonEmpty
      && grades(universityId).exists(_.grade == selectedGrades(universityId))
    ) {
      grades(universityId).find(_.grade == selectedGrades(universityId))
    } else {
      if (!useDefaultGradeForZero && mark == "0") {
        None // TAB-3499
      } else {
        grades(universityId).find(_.isDefault)
      }
    }
  }

  @RequestMapping(method = Array(POST))
  def post(
    @ModelAttribute("command") cmd: GenerateGradesFromMarkCommand,
    errors: Errors
  ): Mav = {
    val result = cmd.apply()
    if (result.nonEmpty) {
      val universityId = result.keys.head
      val default = defaultGrade(universityId, cmd.studentMarks.asScala.toMap, result, cmd.selected.asScala.toMap, cmd.assessment.module.adminDepartment.assignmentGradeValidationUseDefaultForZero)

      Mav("_generatedGrades",
        "grades" -> result.values.toSeq.headOption.getOrElse(Seq()).sorted,
        "default" -> default
      ).noLayout()
    } else {
      Mav("_generatedGrades",
        "grades" -> Seq(),
        "default" -> null
      ).noLayout()
    }
  }

  @RequestMapping(value = Array("/multiple"), method = Array(POST))
  def postMultiple(@ModelAttribute("command") cmd: GenerateGradesFromMarkCommand): Mav = {
    val result = cmd.apply()
    val defaults = result.keys.map(universityId => universityId ->
      defaultGrade(universityId, cmd.studentMarks.asScala.toMap, result, cmd.selected.asScala.toMap, cmd.assessment.module.adminDepartment.assignmentGradeValidationUseDefaultForZero)
    ).toMap

    Mav("generatedGrades",
      "result" -> result,
      "defaults" -> defaults
    ).noLayout()
  }

}



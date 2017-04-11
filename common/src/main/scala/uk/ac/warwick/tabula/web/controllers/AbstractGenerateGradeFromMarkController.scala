package uk.ac.warwick.tabula.web.controllers

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommandRequest
import uk.ac.warwick.tabula.data.model.{Assessment, GradeBoundary, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AbstractGenerateGradeFromMarkController.GenerateGradesFromMarkCommand

import scala.collection.JavaConverters._

object AbstractGenerateGradeFromMarkController {
	type GenerateGradesFromMarkCommand = Appliable[Map[String, Seq[GradeBoundary]]] with GenerateGradesFromMarkCommandRequest
}

abstract class AbstractGenerateGradeFromMarkController[A <: Assessment] extends BaseController {

	def command(module: Module, assessment: A): GenerateGradesFromMarkCommand

	def defaultGrade(
		universityId: String,
		marks: Map[String, String],
		grades: Map[String, Seq[GradeBoundary]],
		selectedGrades: Map[String, String]
	): Option[GradeBoundary] = {
		val mark = marks(universityId)
		if (selectedGrades.get(universityId).flatMap(_.maybeText).nonEmpty
			&& grades(universityId).exists(_.grade == selectedGrades(universityId))
		) {
			grades(universityId).find(_.grade == selectedGrades(universityId))
		} else {
			if (mark == "0") {
				None // TAB-3499
			} else {
				grades(universityId).find(_.isDefault)
			}
		}
	}

	@RequestMapping(method= Array(POST))
	def post(
		@ModelAttribute("command") cmd: GenerateGradesFromMarkCommand,
		errors: Errors
	): Mav = {
		val result = cmd.apply()
		if (result.nonEmpty) {
			val universityId = result.keys.head
			val default = defaultGrade(universityId, cmd.studentMarks.asScala.toMap, result, cmd.selected.asScala.toMap)

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

	@RequestMapping(value = Array("/multiple"), method= Array(POST))
	def postMultiple(@ModelAttribute("command") cmd: GenerateGradesFromMarkCommand): Mav = {
		val result = cmd.apply()
		val defaults = result.keys.map(universityId => universityId ->
			defaultGrade(universityId, cmd.studentMarks.asScala.toMap, result, cmd.selected.asScala.toMap)
		).toMap

		Mav("generatedGrades",
			"result" -> result,
			"defaults" -> defaults
		).noLayout()
	}

}



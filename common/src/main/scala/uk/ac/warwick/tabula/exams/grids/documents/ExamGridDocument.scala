package uk.ac.warwick.tabula.exams.grids.documents

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.StatusAdapter
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridColumnOption

object ExamGridDocument {
	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Seq[ExamGridColumnOption], Seq[String])] with GenerateExamGridGridOptionsCommandRequest with PopulateOnForm
	type CoreRequiredModulesCommand = Appliable[Map[Route, Seq[CoreRequiredModule]]] with PopulateGenerateExamGridSetCoreRequiredModulesCommand
	type CheckOvercatCommand = Appliable[GenerateExamGridCheckAndApplyOvercatCommand.Result] with GenerateExamGridCheckAndApplyOvercatCommandState with SelfValidating
}

trait ExamGridDocumentPrototype {
	def identifier: String
}

import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._

trait ExamGridDocument {
	def identifier: String

	def contentType: String

	def apply(
		department: Department,
		academicYear: AcademicYear,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommand: CheckOvercatCommand,
		options: Map[String, Any],
		status: StatusAdapter
	): FileAttachment
}

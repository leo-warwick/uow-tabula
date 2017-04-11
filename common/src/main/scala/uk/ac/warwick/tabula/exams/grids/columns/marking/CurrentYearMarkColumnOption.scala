package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class CurrentYearMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val label: String = "Marking: Current year mean mark"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CurrentYear

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Weighted Mean Module Mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entity => result(entity) match {
					case Right(mark) => ExamGridColumnValueDecimal(mark)
					case Left(message) => ExamGridColumnValueMissing(message)
				}).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

		private def result(entity: ExamGridEntityYear): Either[String, BigDecimal] = {
			if (state.overcatSubsets(entity).size > 1 && entity.overcattingModules.isEmpty) {
				// If the has more than one valid overcat subset, and a subset has not been chosen for the overcatted mark, don't show anything
				Left("The overcat adjusted mark subset has not been chosen")
			} else {
				moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations, entity.markOverrides.getOrElse(Map()))
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

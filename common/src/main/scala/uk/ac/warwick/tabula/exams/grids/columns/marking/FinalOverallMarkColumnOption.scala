package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringProgressionServiceComponent, FinalYearGrade, FinalYearMark}

@Component
class FinalOverallMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "finalOverallMark"

	override val label: String = "Marking: Final overall mark"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.FinalOverallMark

	case class Column(state: ExamGridColumnState)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Final Overall Mark"

		override val category: String = "Marking"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entityYear =>
					progressionService.suggestedFinalYearGrade(
						entityYear.studentCourseYearDetails.get,
						state.normalLoadLookup(entityYear.route),
						state.routeRulesLookup(entityYear.route)
					) match {
						case unknown: FinalYearGrade.Unknown => ExamGridColumnValueMissing(unknown.details)
						case withMark: FinalYearMark => ExamGridColumnValueDecimal(withMark.mark)
						case result => ExamGridColumnValueString(result.description)
					}
				).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

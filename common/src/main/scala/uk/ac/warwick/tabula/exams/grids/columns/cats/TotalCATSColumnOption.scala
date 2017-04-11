package uk.ac.warwick.tabula.exams.grids.columns.cats

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class TotalCATSColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "totalCats"

	override val label: String = "CATS breakdowns: Total"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.TotalCATS

	case class Column(state: ExamGridColumnState)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Total"

		override val category: String = "CATS breakdowns"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entityYear =>
					ExamGridColumnValueDecimal(entityYear.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying)
				).getOrElse(
					ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}")
				)
			).toMap
		}
	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

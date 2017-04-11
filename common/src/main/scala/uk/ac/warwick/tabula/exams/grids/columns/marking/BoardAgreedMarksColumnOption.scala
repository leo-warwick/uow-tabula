package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class BoardAgreedMarksColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "board"

	override val label: String = "Marking: Board agreed mark"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.BoardAgreedMark

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Board Agreed Mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueString(
					entity.years.get(state.yearOfStudy)
						.flatMap(_.studentCourseYearDetails)
						.flatMap(scyd => Option(scyd.agreedMark))
						.map(_.toPlainString).getOrElse("")
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

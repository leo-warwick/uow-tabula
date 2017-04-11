package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class CommentsColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "comments"

	override val label: String = "Administration: Comments"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.Comments

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Comments"

		override val category: String = "Administration"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.ShortString

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity -> ExamGridColumnValueString("")).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

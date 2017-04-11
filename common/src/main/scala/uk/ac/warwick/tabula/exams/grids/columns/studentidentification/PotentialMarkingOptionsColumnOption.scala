package uk.ac.warwick.tabula.exams.grids.columns.studentidentification

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._

@Component
class PotentialMarkingOptionsColumnOption extends StudentExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "markingoptions"

	override val label: String = ""

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.PotentialMarkingOptions

	override val mandatory = true

	case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) {

		override val title: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity ->
				ExamGridColumnValueStringHtmlOnly(
					"<button class=\"btn btn-default edit-overcatting btn-xs\" type=\"button\" data-student=\"%s\">Edit</button>".format(
						entity.years(state.yearOfStudy).studentCourseYearDetails.get.id
					)
				)
			).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

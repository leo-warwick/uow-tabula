package uk.ac.warwick.tabula.exams.grids.columns.actions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringProgressionServiceComponent, FinalYearGrade}

@Component
class SuggestedFinalYearGradeColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "suggestedgrade"

  override val label: String = "Suggested Actions: Suggested final year grade"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.SuggestedFinalYearGrade

  case class Column(state: ExamGridColumnState)
    extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Suggested Final Year Grade"

    override val category: String = "Suggested Actions"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.years.filter { case (_, entityYear) => entityYear.nonEmpty }.get(state.yearOfStudy).map(entityYear =>
          progressionService.suggestedFinalYearGrade(
            entityYear.get,
            state.normalLoadLookup(entityYear.get.route),
            entity.validYears.view.mapValues(ey => state.routeRulesLookup(ey.route, ey.level)).toMap,
            state.yearMarksToUse,
            state.isLevelGrid,
            state.applyBenchmark,
            entity.yearWeightings
          ) match {
            case unknown: FinalYearGrade.Unknown => ExamGridColumnValueMissing(unknown.reason)
            case result if result.details.nonEmpty => ExamGridColumnValueWithTooltip(result.description, message = result.details.get)
            case result => ExamGridColumnValueString(result.description)
          }
        ).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

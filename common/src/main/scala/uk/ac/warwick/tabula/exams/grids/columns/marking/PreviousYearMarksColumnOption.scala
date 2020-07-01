package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, AutowiringProgressionServiceComponent}

@Component
class PreviousYearMarksColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringProgressionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "previous"

  override val label: String = "Marking: Marks from previous year(s)"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.PreviousYears

  case class Column(state: ExamGridColumnState, thisYearOfStudy: Int) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = s"Year $thisYearOfStudy"

    override val category: String = "Previous Year Marks"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> (markOrError(entity) match {
          case Right(mark) => ExamGridColumnValueDecimal(mark)
          case Left(error) => ExamGridColumnValueMissing(error)
        })
      ).toMap
    }

    private def markOrError(entity: ExamGridEntity): Either[String, BigDecimal] = {
      relevantEntityYear(entity) match {
        case Some(year) =>
          lazy val uploadedYearMark: Option[BigDecimal] =
            Option(year.studentCourseYearDetails.get.agreedMark).map(BigDecimal(_))

          lazy val calculatedYearMark: Either[String, BigDecimal] =
            progressionService.getYearMark(year, state.normalLoadLookup(year.route), state.routeRulesLookup(year.route, year.level), entity.yearWeightings)

          state.yearMarksToUse match {
            case ExamGridYearMarksToUse.UploadedYearMarksOnly =>
              uploadedYearMark.toRight(s"No year mark for Year ${year.yearOfStudy}")

            case ExamGridYearMarksToUse.UploadedYearMarksIfAvailable =>
              uploadedYearMark.fold(calculatedYearMark)(Right.apply)

            case ExamGridYearMarksToUse.CalculateYearMarks =>
              calculatedYearMark
          }

        case _ => Left(s"No course detail found for ${entity.universityId} for Year $thisYearOfStudy")
      }
    }

    /**
      * Gets the ExamGridEntityYear for this previous year of study.
      * This may have already been calculated if we're showing previous year registrations.
      * If not we need to re-fetch it.
      */
    private def relevantEntityYear(entity: ExamGridEntity): Option[ExamGridEntityYear] = {
      entity.validYears.get(thisYearOfStudy).orElse(
        entity.validYears.values.lastOption.flatMap(entityYear =>
          // For the last year go back up to the student and re-fetch the ExamGridEntity
          entityYear.studentCourseYearDetails.get.studentCourseDetails.student.toExamGridEntity(entityYear.studentCourseYearDetails.get)
            // Then see if a matching ExamGrdEntityYear exists
            .validYears.get(thisYearOfStudy)
        )
      )
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = {
    val requiredYears = 1 until state.yearOfStudy
    requiredYears.map(year => Column(state, year))
  }

}

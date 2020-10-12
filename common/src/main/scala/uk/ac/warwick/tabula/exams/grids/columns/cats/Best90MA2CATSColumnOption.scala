package uk.ac.warwick.tabula.exams.grids.columns.cats

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.exams.grids.columns.{HasExamGridColumnCategory, _}
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent


abstract class Best90MA2CATSColumnOption(isResultRequired: Boolean = false, columnTitle: String) extends ChosenYearExamGridColumnOption
  with AutowiringModuleRegistrationServiceComponent {


  override val identifier: ExamGridColumnOption.Identifier = "best90MA2Modules"

  override val label: String = "Marking: MA2XX Weighted Average [best 90 MA2XX CATS]"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.Best90CATSWeightedAverage

  final val ProgressCourseTitle = "Progresses on G103 (4 year programme)"

  final val TransferCourseTitle = "Transferred to G100 (3 year programme)"

  // extract weighted mean mark for selected records...
  def weightedMeanYearMarks(records: Seq[ModuleRegistration]): Either[String, BigDecimal] = {
    moduleRegistrationService.weightedMeanYearMark(records, Map(), allowEmpty = false)
  }


  def best90MAModuleSet(
    records: Seq[ModuleRegistration]
  ): Seq[(BigDecimal, Seq[ModuleRegistration])] = {
    //get all valid subsets
    val validSubsets = records.toSet.subsets().toSeq.filter(_.nonEmpty).filter(modRegs =>
      // CATS total of 90 subsets
      modRegs.toSeq.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum == 90
    )

    val validSubsetsWithWieghtedMeanMark = validSubsets.map(modRegs => (weightedMeanYearMarks(modRegs.toSeq), modRegs.toSeq.sortBy(_.module.code)))
    validSubsetsWithWieghtedMeanMark.collect { case (Right(mark), modRegs) => (mark, modRegs) }
      .sortBy { case (mark, modRegs) =>
        // Add a definitive sort so subsets with the same mark always come out the same order
        (mark.doubleValue, modRegs.size, modRegs.map(_.module.code).mkString(","))
      }.reverse
  }


  def getResultTitle(mark: BigDecimal): String = {
    if (mark < 65) TransferCourseTitle else ProgressCourseTitle
  }

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val category: String = "MA2XX Modules"
    override val title: String = columnTitle

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      //add logic to display best 90 cats for each ExamGridEntity
      if (state.department.rootDepartment.code == "ma") {
        state.entities.map { entity =>
          //get all the MA2 modules for the student (2nd year)
          val validRecords = entity.years.get(2).flatten match {
            case Some(examGridEntityYear) => examGridEntityYear.moduleRegistrations.filter(mr => mr.module.code.toUpperCase.startsWith("MA2") && !mr.deleted && mr.firstDefinedMark.isDefined)
            case None => Seq()
          }
          //if cats are <= 90 then only one possible option
          if (validRecords.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum <= 90) {
            val gridColumnValue = weightedMeanYearMarks(validRecords) match {
              case Right(mark) => if (isResultRequired) ExamGridColumnValueString(getResultTitle(mark)) else ExamGridColumnValueDecimal(mark)
              case Left(message) => ExamGridColumnValueMissing(message)
            }
            entity -> gridColumnValue
          } else {
            //get the best one out
            val mark = best90MAModuleSet(validRecords).headOption.map(_._1).getOrElse(BigDecimal(0))
            if (isResultRequired) {
              entity -> ExamGridColumnValueString(getResultTitle(mark))
            } else {
              entity -> ExamGridColumnValueDecimal(mark)
            }
          }
        }.toMap
      } else {
        state.entities.map(entity => entity -> ExamGridColumnValueString("")).toMap
      }
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}

@Component
class Best90MA2WeightAverageMarksColumn extends Best90MA2CATSColumnOption(columnTitle = s"Weighted Average Best 90 CATS") {
  override val sortOrder: Int = ExamGridColumnOption.SortOrders.Best90CATSWeightedAverage
}

@Component
class Best90MA2CourseStatusColumn extends Best90MA2CATSColumnOption(true, "Year 2 Course status") {
  override val sortOrder: Int = ExamGridColumnOption.SortOrders.Best90CATSResult

}







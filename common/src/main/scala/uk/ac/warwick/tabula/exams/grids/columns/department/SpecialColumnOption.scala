package uk.ac.warwick.tabula.exams.grids.columns.department

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.{CourseYearWeighting, ModuleRegistration}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

import scala.math.BigDecimal.RoundingMode

@Component
class SpecialColumnOption extends ChosenYearExamGridColumnOption  with AutowiringModuleRegistrationServiceComponent{

	override val identifier: ExamGridColumnOption.Identifier = "best90MA2Modules"

	override val label: String = "Marking: MA2XX Weighted Average [best 90 MA2XX CATS]"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.BEST90CATS

	case class Column(state: ExamGridColumnState)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = s"Weighted Average Best 90 CATS"

		override val category: String = "MA2XX Modules"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			//add logic to display best 90 cats for each ExamGridEntity
			if (state.department.code == "ma" || Option(state.department.parent).exists(_.code == "ma")) {
				state.entities.map { entity =>
					//get all the MA2 modules for the student
					var validRecords = entity.years.get(2).flatten.get.moduleRegistrations.filter(_.module.code.toUpperCase.startsWith("MA2") )

				//if cats are <= 90 then only one possible option
					if (validRecords.map(mr => BigDecimal(mr.cats)).sum <= 90) {
						var gridColumnValue = result(validRecords) match {
							case Right(mark) => ExamGridColumnValueDecimal(mark)
							case Left(message) => ExamGridColumnValueMissing(message)
						}
						entity -> gridColumnValue
					} else {
						////lots of possibilitions in which case we need to  calculate all and then get the best one out
						entity -> ExamGridColumnValueString("TEST")
					}
				}.toMap
			} else {
				state.entities.map(entity => entity -> ExamGridColumnValueString("")).toMap
			}
		}

		//extract meanweighted mark for selected records...
		private def result(records: Seq[ModuleRegistration]): Either[String, BigDecimal] = {
			// find out what markOverrides and allowEmpty means :D
			 moduleRegistrationService.weightedMeanYearMark(records, Map(), allowEmpty = true)
		}
	}




	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}

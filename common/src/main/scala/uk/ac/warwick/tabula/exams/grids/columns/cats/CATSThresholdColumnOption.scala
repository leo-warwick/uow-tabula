package uk.ac.warwick.tabula.exams.grids.columns.cats

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.exams.grids.columns._

abstract class CATSThresholdColumnOption(bound: BigDecimal, isUpperBound: Boolean = false) extends ChosenYearExamGridColumnOption {

	case class Column(state: ExamGridColumnState)
		extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = if (isUpperBound) s"<=$bound" else s">=$bound"

		override val category: String = "CATS breakdowns"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entityYear => result(entityYear))
					.getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

		private def result(entity: ExamGridEntityYear): ExamGridColumnValue = {
			def transformModuleRegistrations(moduleRegistrations: Seq[ModuleRegistration]): JBigDecimal = {
				moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum.underlying
			}

			if (entity.moduleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
				ExamGridColumnValueMissing("The total CATS cannot be calculated because the following module registrations have no mark: %s".format(
					entity.moduleRegistrations.filter(_.firstDefinedMark.isEmpty).map(_.module.code.toUpperCase).mkString(", ")
				))
			} else if (isUpperBound) {
				ExamGridColumnValueDecimal(transformModuleRegistrations(entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) <= bound))))
			} else {
				ExamGridColumnValueDecimal(transformModuleRegistrations(entity.moduleRegistrations.filter(mr => mr.firstDefinedMark.exists(mark => BigDecimal(mark) >= bound))))
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}

@Component
class ThirtyCATSColumnOption extends CATSThresholdColumnOption(BigDecimal(30), isUpperBound = true) {
	override val identifier: ExamGridColumnOption.Identifier = "30cats"
	override val label: String = "CATS breakdowns: <= 30"
	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSThreshold30
}

@Component
class FortyCATSColumnOption extends CATSThresholdColumnOption(BigDecimal(40)) {
	override val identifier: ExamGridColumnOption.Identifier = "40cats"
	override val label: String = "CATS breakdowns: >= 40"
	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSThreshold40
}

@Component
class FiftyCATSColumnOption extends CATSThresholdColumnOption(BigDecimal(50)) {
	override val identifier: ExamGridColumnOption.Identifier = "50cats"
	override val label: String = "CATS breakdowns: >= 50"
	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSThreshold50
}

@Component
class SixtyCATSColumnOption extends CATSThresholdColumnOption(BigDecimal(60)) {
	override val identifier: ExamGridColumnOption.Identifier = "60cats"
	override val label: String = "CATS breakdowns: >= 60"
	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSThreshold60
}

@Component
class SeventyCATSColumnOption extends CATSThresholdColumnOption(BigDecimal(70)) {
	override val identifier: ExamGridColumnOption.Identifier = "70cats"
	override val label: String = "CATS breakdowns: >= 70"
	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSThreshold70
}

package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnValueString, _}

import scala.math.BigDecimal.RoundingMode

@Component
class ModuleReportsColumnOption extends PerYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "modulereports"

	override val label: String = "Modules: Module Reports"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.ModuleReports

	case class PassedCoreRequiredColumn(state: ExamGridColumnState)
		extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue {

		override val category: String = "Modules Report"

		override val title: String = "Passed Required Core Modules?"

		override val secondaryValue: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

		override def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]] = {
			state.entities.map(entity =>
				entity -> entity.years.map { case (academicYear, entityYear) =>
					academicYear -> ExamGridColumnValueType.toMap(result(entityYear))
				}
			).toMap
		}

		private def result(entity: ExamGridEntityYear): ExamGridColumnValue = {
			val coreRequiredModules = state.coreRequiredModuleLookup(entity.route).map(_.module)
			val coreRequiredModuleRegistrations = entity.moduleRegistrations.filter(mr => coreRequiredModules.contains(mr.module))
			if (coreRequiredModules.nonEmpty) {
				if (coreRequiredModuleRegistrations.exists(_.agreedGrade == "F")) {
					ExamGridColumnValueString("N")
				} else {
					ExamGridColumnValueString("Y")
				}
			} else {
				ExamGridColumnValueString("")
			}
		}

	}

	case class MeanModuleMarkColumn(state: ExamGridColumnState)
		extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue {

		override val category: String = "Modules Report"

		override val title: String = "Mean Module Mark For This Year"

		override val secondaryValue: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]] = {
			state.entities.map(entity =>
				entity -> entity.years.map { case (academicYear, entityYear) =>
					academicYear -> ExamGridColumnValueType.toMap(result(entityYear))
				}
			).toMap
		}

		private def result(entity: ExamGridEntityYear): ExamGridColumnValue = {
			val entityMarks = entity.moduleRegistrations.flatMap(mr => mr.firstDefinedMark).map(mark => BigDecimal(mark))
			if (entityMarks.nonEmpty) {
				ExamGridColumnValueString((entityMarks.sum / entityMarks.size).setScale(1, RoundingMode.HALF_UP).toString)
			} else {
				ExamGridColumnValueString("")
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]] = {
		state.entities.flatMap(_.years.keys).distinct.map(academicYear => academicYear -> Seq(
			PassedCoreRequiredColumn(state),
			MeanModuleMarkColumn(state)
		)).toMap
	}
}

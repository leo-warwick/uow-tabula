package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, ModuleSelectionStatus, UpstreamAssessmentGroupMember}
import uk.ac.warwick.tabula.exams.grids.columns._

abstract class ModuleExamGridColumn(state: ExamGridColumnState, val module: Module, isDuplicate: Boolean, cats: JBigDecimal)
	extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue {

	def moduleSelectionStatus: Option[ModuleSelectionStatus]

	override val title: String =
		if (state.showModuleNames) s"${module.code.toUpperCase} ${module.name}"
		else s"${module.code.toUpperCase}"

	override val boldTitle: Boolean = isDuplicate

	override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

	override def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]] = {
		state.entities.map(entity =>
			entity -> entity.years.map { case (academicYear, entityYear) =>
				academicYear -> result(entityYear)
			}
		).toMap
	}

	private def result(entity: ExamGridEntityYear): Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]] = {
		getModuleRegistration(entity).map(mr => {
			val overallMark = {
				if (entity.markOverrides.exists(_.get(module).isDefined)) {
					ExamGridColumnValueOverrideDecimal(entity.markOverrides.get(module))
				} else {
					val (mark, isActual) = mr.agreedMark match {
						case mark: JBigDecimal => (BigDecimal(mark), false)
						case _ => Option(mr.actualMark).map(m => (BigDecimal(m), true)).getOrElse(null, false)
					}
					if (mark == null) {
						ExamGridColumnValueMissing("Agreed and actual mark missing")
					} else if (isActual && mr.actualGrade == "F" || mr.agreedGrade == "F") {
						ExamGridColumnValueFailedDecimal(mark, isActual)
					} else if (entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))) {
						ExamGridColumnValueOvercatDecimal(mark, isActual)
					} else {
						ExamGridColumnValueDecimal(mark, isActual)
					}
				}
			}
			if (state.showComponentMarks) {
				def toValue(member: UpstreamAssessmentGroupMember): ExamGridColumnValue = {
					val (mark, isActual) = member.agreedMark match {
						case Some(agreedMark) => (agreedMark, false)
						case _ => member.actualMark.map(actualMark => (actualMark, true)).getOrElse(null, false)
					}
					if (mark == null) {
						ExamGridColumnValueMissing("Agreed and actual mark missing")
					} else if (isActual && member.actualGrade.contains("F") || member.agreedGrade.contains("F")) {
						ExamGridColumnValueFailedDecimal(mark, isActual)
					} else {
						ExamGridColumnValueDecimal(mark, isActual)
					}
				}

				val (exams, assignments) = mr.upstreamAssessmentGroupMembers.filter(m => m.agreedMark.isDefined || m.actualMark.isDefined)
					.partition(_.upstreamAssessmentGroup.sequence.startsWith("E"))

				ExamGridColumnValueType.toMap(
					overall = overallMark,
					assignments = assignments.map(toValue),
					exams = exams.map(toValue)
				)
			} else {
				ExamGridColumnValueType.toMap(overallMark)
			}

		}).getOrElse(
			ExamGridColumnValueType.toMap(ExamGridColumnValueString(""))
		)
	}

	private def getModuleRegistration(entity: ExamGridEntityYear): Option[ModuleRegistration] = {
		entity.moduleRegistrations.find(mr =>
			mr.module == module &&
				mr.cats == cats &&
				(moduleSelectionStatus.isEmpty || mr.selectionStatus == moduleSelectionStatus.get)
		)
	}

	override val secondaryValue: String = cats.toPlainString

}

abstract class ModuleExamGridColumnOption extends PerYearExamGridColumnOption {

	// isDuplicate is true if this module appears more than once in the grid (as a core and optional module for example)
	def Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn

	def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean

	override final def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]] = {

		val allYears = state.entities.flatMap(_.years.values).toList

		val allCoreRequiredModules =
			allYears.map(_.route).flatMap(state.coreRequiredModuleLookup.apply).map(cr => (cr.module, "CoreRequired"))

		val allOtherModules = allYears
			.flatMap(_.moduleRegistrations).map(mr => (mr.module, mr.selectionStatus))

		val duplicateModules = (allCoreRequiredModules ++ allOtherModules).distinct
			.groupBy { case (module, _) => module }
			// collect modules with 2 or more different selection statuses
			.collect { case (module, _ :: _ :: _) => module }
			.toSeq

		state.entities.flatMap(_.years.keys).distinct.map(academicYear => academicYear -> {
			val years = state.entities.flatMap(_.years.get(academicYear))
			val moduleAndCats: Set[(Module, JBigDecimal)] = years.flatMap { entityYear =>
				val coreRequiredModules = state.coreRequiredModuleLookup(entityYear.route).map(_.module)
				entityYear.moduleRegistrations.filter(moduleRegistrationFilter(_, coreRequiredModules))
			}.groupBy(mr => (mr.module, mr.cats)).keySet

			moduleAndCats
				.toSeq
				.sortBy { case (module, cats) => (module, cats) }
				.map{ case (module, cats) => Column(state, module, duplicateModules.contains(module), cats)}
		}).toMap
	}
}

@Component
class CoreModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "core"

	override val label: String = "Modules: Core Modules"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreModules

	override val mandatory = true

	class Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal)
		extends ModuleExamGridColumn(state, module, isDuplicate, cats) {

		override val category: String = "Core Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.Core)

	}

	override def Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
		= new Column(state, module, isDuplicate, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.Core && !coreRequiredModules.contains(mr.module)

}

@Component
class CoreRequiredModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "corerequired"

	override val label: String = "Modules: Core Required Modules"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreRequiredModules

	class Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal)
		extends ModuleExamGridColumn(state, module, isDuplicate, cats) {

		override val category: String = "Core Required Modules"

		override val moduleSelectionStatus = None

	}

	override def Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
		= new Column(state, module, isDuplicate, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
		coreRequiredModules.contains(mr.module)

}

@Component
class CoreOptionalModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

	override val label: String = "Modules: Core Optional Modules"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreOptionalModules

	class Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal)
		extends ModuleExamGridColumn(state, module, isDuplicate, cats) {

		override val category: String = "Core Optional Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.OptionalCore)

	}

	override def Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
		= new Column(state, module, isDuplicate, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !coreRequiredModules.contains(mr.module)

}

@Component
class OptionalModulesColumnOption extends ModuleExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "optional"

	override val label: String = "Modules: Optional Modules"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.OptionalModules

	class Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal)
		extends ModuleExamGridColumn(state, module, isDuplicate, cats) {

		override val category: String = "Optional Modules"

		override val moduleSelectionStatus = Option(ModuleSelectionStatus.Option)

	}

	override def Column(state: ExamGridColumnState, module: Module, isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
		= new Column(state, module, isDuplicate, cats)

	override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
		mr.selectionStatus == ModuleSelectionStatus.Option && !coreRequiredModules.contains(mr.module)

}
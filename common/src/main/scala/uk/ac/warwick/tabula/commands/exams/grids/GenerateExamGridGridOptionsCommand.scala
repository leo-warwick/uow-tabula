package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings.ExamGridOptions
import uk.ac.warwick.tabula.exams.grids.columns.marking.CurrentYearMarkColumnOption
import uk.ac.warwick.tabula.exams.grids.columns.modules.CoreModulesColumnOption
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnOption, ExamGridDisplayModuleNameColumnValue, ExamGridStudentIdentificationColumnValue}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object GenerateExamGridGridOptionsCommand {
	def apply(department: Department) =
		new GenerateExamGridGridOptionsCommandInternal(department)
			with ComposableCommand[(Seq[ExamGridColumnOption], Seq[String])]
			with AutowiringModuleAndDepartmentServiceComponent
			with PopulatesGenerateExamGridGridOptionsCommand
			with GenerateExamGridGridOptionsValidation
			with GenerateExamGridGridOptionsPermissions
			with GenerateExamGridGridOptionsDescription
			with GenerateExamGridGridOptionsCommandState
			with GenerateExamGridGridOptionsCommandRequest
			with GenerateExamGridGridOptionsCommandDepartmentPersistence

	def applyReadOnly(department: Department) =
		new GenerateExamGridGridOptionsCommandInternal(department)
			with ComposableCommand[(Seq[ExamGridColumnOption], Seq[String])]
			with AutowiringModuleAndDepartmentServiceComponent
			with PopulatesGenerateExamGridGridOptionsCommand
			with GenerateExamGridGridOptionsValidation
			with GenerateExamGridGridOptionsPermissions
			with GenerateExamGridGridOptionsDescription
			with GenerateExamGridGridOptionsCommandState
			with GenerateExamGridGridOptionsCommandRequest
			with GenerateExamGridGridOptionsCommandNoPersistence
			with ReadOnly
}

class GenerateExamGridGridOptionsCommandInternal(val department: Department) extends CommandInternal[(Seq[ExamGridColumnOption], Seq[String])] {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest with GenerateExamGridGridOptionsCommandPersistence =>

	override def applyInternal(): (Seq[ExamGridColumnOption], Seq[String]) = {
		department.examGridOptions = ExamGridOptions(
			predefinedColumnIdentifiers.asScala.toSet,
			customColumnTitles.asScala,
			nameToShow,
			marksToShow,
			componentsToShow,
			componentSequenceToShow,
			moduleNameToShow,
			layout,
			yearMarksToUse,
			mandatoryModulesAndYearMarkColumns
		)

		saveDepartment()

		(predefinedColumnOptions, customColumnTitles.asScala)
	}

}

trait GenerateExamGridGridOptionsCommandPersistence {
	def saveDepartment(): Unit
}

trait GenerateExamGridGridOptionsCommandNoPersistence extends GenerateExamGridGridOptionsCommandPersistence {
	self: ReadOnly =>
	override def saveDepartment(): Unit = {}
}

trait GenerateExamGridGridOptionsCommandDepartmentPersistence extends GenerateExamGridGridOptionsCommandPersistence {
	self: GenerateExamGridGridOptionsCommandState with ModuleAndDepartmentServiceComponent =>
	override def saveDepartment(): Unit = {
		moduleAndDepartmentService.saveOrUpdate(department)
	}
}

trait PopulatesGenerateExamGridGridOptionsCommand extends PopulateOnForm {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest =>

	def populate(): Unit = {
		val options = department.examGridOptions
		predefinedColumnIdentifiers.clear()
		predefinedColumnIdentifiers.addAll(options.predefinedColumnIdentifiers.asJava)
		customColumnTitles.clear()
		customColumnTitles.addAll(options.customColumnTitles.asJava)
		nameToShow = options.nameToShow
		marksToShow = options.marksToShow
		componentsToShow = options.componentsToShow
		componentSequenceToShow = options.componentSequenceToShow
		moduleNameToShow = options.moduleNameToShow
		layout = options.layout
		yearMarksToUse = options.yearMarksToUse
		mandatoryModulesAndYearMarkColumns = options.mandatoryModulesAndYearMarkColumns

		if (options.mandatoryModulesAndYearMarkColumns) {
			predefinedColumnIdentifiers.add(new CoreModulesColumnOption().identifier)
			predefinedColumnIdentifiers.add(new CurrentYearMarkColumnOption().identifier)
		}
	}
}

trait GenerateExamGridGridOptionsValidation extends SelfValidating {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest =>

	override def validate(errors: Errors) {
		val allIdentifiers = allExamGridsColumns.map(_.identifier).toSet
		val invalidColumns = predefinedColumnIdentifiers.asScala.diff(allIdentifiers)
		if (invalidColumns.nonEmpty) {
			errors.reject("examGrid.invalidColumns", Array(invalidColumns.mkString(", ")), "")
		}
	}

}

trait GenerateExamGridGridOptionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridGridOptionsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridGridOptionsDescription extends Describable[(Seq[ExamGridColumnOption], Seq[String])] {

	self: GenerateExamGridGridOptionsCommandState =>

	override lazy val eventName = "GenerateExamGridGridOptions"

	override def describe(d: Description) {
		d.department(department)
	}

	override def describeResult(d: Description, result: (Seq[ExamGridColumnOption], Seq[String])): Unit = {
		d.property("predefined", result._1.map(_.identifier)).property("custom", result._2)
	}
}

trait GenerateExamGridGridOptionsCommandState {
	def department: Department
	var allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted
}

trait GenerateExamGridGridOptionsCommandRequest {

	self: GenerateExamGridGridOptionsCommandState =>

	var predefinedColumnIdentifiers: JSet[String] = JHashSet()
	var nameToShow: ExamGridStudentIdentificationColumnValue = ExamGridStudentIdentificationColumnValue.FullName
	var marksToShow: String = "overall"
	var componentsToShow: String = "all"
	var componentSequenceToShow: String = "markOnly"
	var moduleNameToShow: ExamGridDisplayModuleNameColumnValue = ExamGridDisplayModuleNameColumnValue.NoNames
	var layout: String = "full"
	var yearMarksToUse: String = "sits"
	var mandatoryModulesAndYearMarkColumns: Boolean = true
	var customColumnTitles: JList[String] = JArrayList()

	def showFullLayout: Boolean = layout == "full"
	def showComponentMarks: Boolean = marksToShow == "all"
	def showComponentSequence: Boolean = componentSequenceToShow == "sequenceAndMark"
	def showZeroWeightedComponents: Boolean = componentsToShow == "all"
	def calculateYearMarks: Boolean = yearMarksToUse != "sits"

	private def isModulesOrYearMarkColumn(identifier: ExamGridColumnOption.Identifier): Boolean =
		identifier == new CoreModulesColumnOption().identifier ||
		identifier == new CurrentYearMarkColumnOption().identifier

	protected lazy val predefinedColumnOptions: Seq[ExamGridColumnOption] =
		allExamGridsColumns.filter(c => c.mandatory || predefinedColumnIdentifiers.contains(c.identifier) || (mandatoryModulesAndYearMarkColumns && isModulesOrYearMarkColumn(c.identifier)))

	lazy val predefinedColumnDescriptions: Seq[String] = {
		predefinedColumnOptions.filter(_.label.nonEmpty).map(_.label)
	}

	def toMap: Map[String, Any] = Map(
		"predefinedColumnIdentifiers" -> predefinedColumnIdentifiers,
		"nameToShow" -> nameToShow.value,
		"marksToShow" -> marksToShow,
		"componentsToShow" -> componentsToShow,
		"componentSequenceToShow" -> componentSequenceToShow,
		"moduleNameToShow" -> moduleNameToShow.value,
		"layout" -> layout,
		"yearMarksToUse" -> yearMarksToUse,
		"mandatoryModulesAndYearMarkColumns" -> mandatoryModulesAndYearMarkColumns,
		"customColumnTitles" -> customColumnTitles
	)
}
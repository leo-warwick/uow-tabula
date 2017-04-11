package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.Settings.ExamGridOptions
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridColumnOption
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
			
}


class GenerateExamGridGridOptionsCommandInternal(val department: Department) extends CommandInternal[(Seq[ExamGridColumnOption], Seq[String])] {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest with ModuleAndDepartmentServiceComponent =>

	override def applyInternal(): (Seq[ExamGridColumnOption], Seq[String]) = {
		department.examGridOptions = ExamGridOptions(
			predefinedColumnIdentifiers.asScala.toSet,
			customColumnTitles.asScala,
			nameToShow,
			yearsToShow,
			marksToShow,
			moduleNameToShow
		)
		moduleAndDepartmentService.saveOrUpdate(department)
		(predefinedColumnOptions, customColumnTitles.asScala)
	}

}

trait PopulatesGenerateExamGridGridOptionsCommand extends PopulateOnForm {

	self: GenerateExamGridGridOptionsCommandState with GenerateExamGridGridOptionsCommandRequest =>

	def populate(): Unit = {
		val options = department.examGridOptions
		predefinedColumnIdentifiers.addAll(options.predefinedColumnIdentifiers.asJava)
		customColumnTitles.addAll(options.customColumnTitles.asJava)
		nameToShow = options.nameToShow
		yearsToShow = options.yearsToShow
		marksToShow = options.marksToShow
		moduleNameToShow = options.moduleNameToShow
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
	var nameToShow: String = "full"
	var yearsToShow: String = "current"
	var marksToShow: String = "overall"
	var moduleNameToShow: String = "codeOnly"
	var customColumnTitles: JList[String] = JArrayList()

	def showFullName: Boolean = nameToShow != "both"
	def showComponentMarks: Boolean = marksToShow == "all"
	def showModuleNames: Boolean = moduleNameToShow == "nameAndCode"

	protected lazy val predefinedColumnOptions: Seq[ExamGridColumnOption] =
		allExamGridsColumns.filter(c => c.mandatory || predefinedColumnIdentifiers.contains(c.identifier))

	lazy val predefinedColumnDescriptions: Seq[String] = {
		predefinedColumnOptions.filter(_.label.nonEmpty).map(_.label)
	}
}
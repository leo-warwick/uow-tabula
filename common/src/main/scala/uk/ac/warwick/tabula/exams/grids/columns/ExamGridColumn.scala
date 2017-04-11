package uk.ac.warwick.tabula.exams.grids.columns

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

object ExamGridColumnOption {
	type Identifier = String
	implicit val defaultOrdering: Ordering[ExamGridColumnOption] = Ordering.by { columnOption: ExamGridColumnOption => columnOption.sortOrder }

	object SortOrders {
		val PotentialMarkingOptions = 0
		val Name = 1
		val UniversityId = 2
		val SPRCode = 3
		val Route = 4
		val StartYear = 5
		val CoreModules = 10
		val CoreRequiredModules = 11
		val CoreOptionalModules = 12
		val OptionalModules = 13
		val ModuleReports = 14
		val CATSThreshold30 = 20
		val CATSThreshold40 = 21
		val CATSThreshold50 = 22
		val CATSThreshold60 = 23
		val CATSThreshold70 = 24
		val TotalCATS = 25
		val PassedCATS = 26
		val PreviousYears = 30
		val CurrentYear = 31
		val OvercattedYearMark = 32
		val BoardAgreedMark = 33
		val FinalOverallMark = 34
		val SuggestedResult = 40
		val SuggestedFinalYearGrade = 41
		val MitigatingCircumstances = 50
		val Comments = 60
	}

	object ExcelColumnSizes {
		val Spacer = 600
		val WholeMark = 900
		val Decimal = 1200
		val ShortString = 1700
		val LongString = 4000
	}
}

case class ExamGridColumnState(
	entities: Seq[ExamGridEntity],
	overcatSubsets: Map[ExamGridEntityYear, Seq[(BigDecimal, Seq[ModuleRegistration])]],
	coreRequiredModuleLookup: CoreRequiredModuleLookup,
	normalLoadLookup: NormalLoadLookup,
	routeRulesLookup: UpstreamRouteRuleLookup,
	academicYear: AcademicYear,
	yearOfStudy: Int,
	showFullName: Boolean,
	showComponentMarks: Boolean,
	showModuleNames: Boolean
)

case object EmptyExamGridColumnState {
	def apply() = ExamGridColumnState(Nil,Map.empty,null,null,null,null,0,showFullName=true,showComponentMarks=false,showModuleNames=true)
}

@Component
sealed trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val label: String
	val sortOrder: Int
	val mandatory: Boolean = false

}

trait StudentExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn]
}

trait PerYearExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]]
}

trait ChosenYearExamGridColumnOption extends ExamGridColumnOption {
	def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn]
}

sealed abstract class ExamGridColumn(state: ExamGridColumnState) {
	val title: String
	val boldTitle: Boolean = false
	val excelColumnWidth: Int
}

abstract class PerYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {
	def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]
}

abstract class ChosenYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {
	def values: Map[ExamGridEntity, ExamGridColumnValue]
}


trait HasExamGridColumnCategory {

	self: ExamGridColumn =>

	val category: String

}

trait HasExamGridColumnSecondaryValue {

	self: ExamGridColumn =>

	val secondaryValue: String

}
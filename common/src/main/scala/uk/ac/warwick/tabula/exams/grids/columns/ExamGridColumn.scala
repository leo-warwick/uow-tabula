package uk.ac.warwick.tabula.exams.grids.columns

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.system.TwoWayConverter

object ExamGridColumnOption {
	type Identifier = String
	implicit val defaultOrdering: Ordering[ExamGridColumnOption] = Ordering.by { columnOption: ExamGridColumnOption => columnOption.sortOrder }

	object SortOrders {
		val PotentialMarkingOptions = 0
		val Name = 1
		val UniversityId = 2
		val SPRCode = 3
		val Course = 4
		val Route = 5
		val StartYear = 6
		val CoreModules = 10
		val CoreRequiredModules = 11
		val CoreOptionalModules = 12
		val OptionalModules = 13
		val ModuleReports = 14
		val BEST90CATS = 19
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

sealed abstract class ExamGridStudentIdentificationColumnValue(val value: String, val description: String) {
	override def toString: String = value
}

object ExamGridStudentIdentificationColumnValue {
	val Default = FullName
	case object NoName  extends ExamGridStudentIdentificationColumnValue("none", "No name")
	case object FullName extends ExamGridStudentIdentificationColumnValue("full", "Official name")
	case object BothName extends ExamGridStudentIdentificationColumnValue("both", "First and last name")

	def fromCode(code: String): ExamGridStudentIdentificationColumnValue =
		if (code == null) null
		else allValues.find{_.value == code} match {
			case Some(identificationType) => identificationType
			case None => throw new IllegalArgumentException()
		}

	def allValues : Seq[ExamGridStudentIdentificationColumnValue] = Seq(NoName, FullName, BothName)

	def apply(value:String): ExamGridStudentIdentificationColumnValue = fromCode(value)
}

class StringToExamGridStudentIdentificationColumnValue extends TwoWayConverter[String, ExamGridStudentIdentificationColumnValue] {
	override def convertRight(source: String): ExamGridStudentIdentificationColumnValue = source.maybeText.map(ExamGridStudentIdentificationColumnValue.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: ExamGridStudentIdentificationColumnValue): String = Option(source).map { _.value }.orNull
}

sealed abstract class ExamGridDisplayModuleNameColumnValue(val value: String, val description: String) {
	override def toString: String = value
}

object ExamGridDisplayModuleNameColumnValue {
	val Default = NoNames
	case object NoNames  extends ExamGridDisplayModuleNameColumnValue("codeOnly", "No names")
	case object LongNames extends ExamGridDisplayModuleNameColumnValue("nameAndCode", "Show module names")
	case object ShortNames extends ExamGridDisplayModuleNameColumnValue("shortNameAndCode", "Show module short names")

	def fromCode(code: String): ExamGridDisplayModuleNameColumnValue =
		if (code == null) null
		else allValues.find{_.value == code} match {
			case Some(displayModuleName) => displayModuleName
			case None => throw new IllegalArgumentException()
		}

	def allValues : Seq[ExamGridDisplayModuleNameColumnValue] = Seq(NoNames, LongNames, ShortNames)

	def apply(value:String): ExamGridDisplayModuleNameColumnValue = fromCode(value)
}

class StringToExamGridDisplayModuleNameColumnValue extends TwoWayConverter[String, ExamGridDisplayModuleNameColumnValue] {
	override def convertRight(source: String): ExamGridDisplayModuleNameColumnValue = source.maybeText.map(ExamGridDisplayModuleNameColumnValue.fromCode).getOrElse(throw new IllegalArgumentException)
	override def convertLeft(source: ExamGridDisplayModuleNameColumnValue): String = Option(source).map { _.value }.orNull
}

case class ExamGridColumnState(
	entities: Seq[ExamGridEntity],
	overcatSubsets: Map[ExamGridEntityYear, Seq[(BigDecimal, Seq[ModuleRegistration])]],
	coreRequiredModuleLookup: CoreRequiredModuleLookup,
	normalLoadLookup: NormalLoadLookup,
	routeRulesLookup: UpstreamRouteRuleLookup,
	academicYear: AcademicYear,
	yearOfStudy: Int,
	department: Department,
	nameToShow: ExamGridStudentIdentificationColumnValue,
	showComponentMarks: Boolean,
	showZeroWeightedComponents: Boolean,
	showComponentSequence: Boolean,
	showModuleNames: ExamGridDisplayModuleNameColumnValue,
	calculateYearMarks: Boolean,
	isLevelGrid: Boolean
)

case object EmptyExamGridColumnState {
	def apply() = ExamGridColumnState(Nil, Map.empty, null, null, null, null, 0, null, nameToShow=ExamGridStudentIdentificationColumnValue.FullName, showComponentMarks=false, showZeroWeightedComponents=false, showComponentSequence=false, showModuleNames=ExamGridDisplayModuleNameColumnValue.LongNames, calculateYearMarks=false, isLevelGrid=false)
}

@Component
sealed trait ExamGridColumnOption {

	val identifier: ExamGridColumnOption.Identifier
	val label: String
	val shortLabel: String = label
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

case class ExamGridColumnValues(values: Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]], isEmpty: Boolean)

abstract class PerYearExamGridColumn(state: ExamGridColumnState) extends ExamGridColumn(state) {

	def values: Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]] = {
		state.entities.map(entity =>
			entity -> entity.validYears.map { case (academicYear, entityYear) =>
				academicYear -> result(entityYear).values
			}
		).toMap
	}

	def isEmpty(entity: ExamGridEntity, year: YearOfStudy): Boolean = entity.validYears.get(year).forall(ey => result(ey).isEmpty)

	def result(entity: ExamGridEntityYear): ExamGridColumnValues
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
package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.JavaImports.JInteger
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.NullStatusAdapter
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.marking.OvercattedYearMarkColumnOption
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreModulesColumnOption, CoreOptionalModulesColumnOption, CoreRequiredModulesColumnOption, OptionalModulesColumnOption}
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalLoadLookup}
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.{ExcelView, JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/overcatting/{scyd}"))
class OvercattingOptionsController extends ExamsController
	with AutowiringModuleRegistrationServiceComponent with AutowiringUpstreamRouteRuleServiceComponent
	with AutowiringNormalCATSLoadServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	private def routeRules(scyd: StudentCourseYearDetails, academicYear: AcademicYear): Seq[UpstreamRouteRule] = {
		scyd.level.map(upstreamRouteRuleService.list(scyd.route, academicYear, _)).getOrElse(Seq())
	}

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable scyd: StudentCourseYearDetails,
		@RequestParam(value = "basedOnLevel", required = false) basedOnLevel: Boolean
	) =
		GenerateExamGridOvercatCommand(
			mandatory(department),
			mandatory(academicYear),
			mandatory(scyd),
			new NormalLoadLookup(academicYear, scyd.yearOfStudy, normalCATSLoadService),
			routeRules(scyd, academicYear),
			user,
			basedOnLevel = basedOnLevel
		)

	@ModelAttribute("overcatView")
	def overcatView(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable scyd: StudentCourseYearDetails,
		@RequestParam(value = "basedOnLevel", required = false) basedOnLevel: Boolean
	) =
		OvercattingOptionsView(department, academicYear, scyd, new NormalLoadLookup(academicYear, scyd.yearOfStudy, normalCATSLoadService), routeRules(scyd, academicYear), basedOnLevel = basedOnLevel)

	@ModelAttribute("ExamGridColumnValueType")
	def examGridColumnValueType = ExamGridColumnValueType

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[Module]] with PopulateOnForm with GenerateExamGridOvercatCommandRequest,
		@ModelAttribute("overcatView") overcatView: OvercattingOptionsView with GenerateExamGridOvercatCommandRequest,
		@PathVariable scyd: StudentCourseYearDetails
	): Mav = {
		cmd.populate()
		overcatView.overcatChoice = cmd.overcatChoice
		Mav("exams/grids/generate/overcat").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.excel))
	def export(
		@ModelAttribute("command") cmd: Appliable[Seq[Module]] with PopulateOnForm with GenerateExamGridOvercatCommandRequest,
		@ModelAttribute("overcatView") overcatView: OvercattingOptionsView with GenerateExamGridOvercatCommandRequest,
		@PathVariable scyd: StudentCourseYearDetails
	): View = {

		cmd.populate()
		overcatView.overcatChoice = cmd.overcatChoice

		new ExcelView(
			s"Overcatting-options-${scyd.studentCourseDetails.student.universityId}.xlsx",
			GenerateExamGridExporter(
				department = overcatView.department,
				academicYear = overcatView.academicYear,
				courses = Seq(scyd.studentCourseDetails.course),
				routes = Seq(scyd.route),
				yearOfStudy = scyd.yearOfStudy,
				yearWeightings = Map(),
				normalLoadLookup = overcatView.normalLoadLookup,
				entities = overcatView.overcattedEntities,
				leftColumns = overcatView.optionsColumns,
				perYearColumns = overcatView.perYearColumns,
				rightColumns = Seq(),
				chosenYearColumnValues = overcatView.optionsColumnValues,
				perYearColumnValues = overcatView.perYearColumnValues,
				showComponentMarks = false,
				yearOrder = Ordering.Int.reverse,
				status = NullStatusAdapter
			)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("recalculate"))
	def recalculate(
		@ModelAttribute("command") cmd: Appliable[Seq[Module]],
		@ModelAttribute("overcatView") overcatView: OvercattingOptionsView
	): Mav = {
		Mav("exams/grids/generate/overcat").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), params = Array("continue"))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[Module]], errors: Errors): JSONView = {
		if (errors.hasErrors) {
			new JSONErrorView(errors)
		} else {
			new JSONView(Map(
				"modules" -> cmd.apply().map(_.code)
			))
		}
	}

}

object OvercattingOptionsView {
	def apply(department: Department, academicYear: AcademicYear, scyd: StudentCourseYearDetails, normalLoadLookup: NormalLoadLookup, routeRules: Seq[UpstreamRouteRule], basedOnLevel: Boolean) =
		new OvercattingOptionsView(department, academicYear, scyd, normalLoadLookup, routeRules, basedOnLevel)
		with AutowiringModuleRegistrationServiceComponent
		with GenerateExamGridOvercatCommandState
		with GenerateExamGridOvercatCommandRequest
}

class OvercattingOptionsView(
	val department: Department,
	val academicYear: AcademicYear,
	val scyd: StudentCourseYearDetails,
	val normalLoadLookup: NormalLoadLookup,
	val routeRules: Seq[UpstreamRouteRule],
	val basedOnLevel: Boolean
) {

	self: GenerateExamGridOvercatCommandState with GenerateExamGridOvercatCommandRequest with ModuleRegistrationServiceComponent =>

	override val user: CurrentUser = null // Never used

	val nameToShow: ExamGridStudentIdentificationColumnValue = department.nameToShow

	private lazy val coreRequiredModuleLookup = new CoreRequiredModuleLookupImpl(academicYear, scyd.yearOfStudy, moduleRegistrationService)

	private lazy val originalEntity = scyd.studentCourseDetails.student.toExamGridEntity(scyd, basedOnLevel)



	def studyYearByLevelOrBlock: JInteger = if(basedOnLevel) {
		Level.toYearOfStudy(scyd.studyLevel)
	} else {
		scyd.yearOfStudy
	}

	lazy val overcattedEntities: Seq[ExamGridEntity] = overcattedModuleSubsets.map { case (_, overcattedModules) =>
		originalEntity.copy(years = originalEntity.years.updated(studyYearByLevelOrBlock, Some(ExamGridEntityYear(
			moduleRegistrations = overcattedModules,
			cats = overcattedModules.map(mr => BigDecimal(mr.cats)).sum,
			route = scyd.studentCourseDetails.currentRoute,
			overcattingModules = Some(overcattedModules.map(_.module)),
			markOverrides = Some(overwrittenMarks),
			studentCourseYearDetails = None,
			level = scyd.level,
			yearOfStudy = studyYearByLevelOrBlock
		))))
	}

	private lazy val overcattedEntitiesState = ExamGridColumnState(
		entities = overcattedEntities,
		overcatSubsets = Map(), // Not used
		coreRequiredModuleLookup = coreRequiredModuleLookup,
		normalLoadLookup = normalLoadLookup,
		routeRulesLookup = null, // Not used
		academicYear = academicYear,
		department = department,
		yearOfStudy = studyYearByLevelOrBlock,
		nameToShow = department.nameToShow,
		showComponentMarks = false,
		showZeroWeightedComponents = false,
		showComponentSequence = false,
		showModuleNames = department.moduleNameToShow,
		calculateYearMarks = false,
		isLevelGrid = basedOnLevel
	)

	private lazy val currentYearMark = moduleRegistrationService.weightedMeanYearMark(allSCYDs.flatMap(_.moduleRegistrations), overwrittenMarks, allowEmpty = false)

	lazy val optionsColumns: Seq[ChosenYearExamGridColumn] = Seq(
		new ChooseOvercatColumnOption().getColumns(overcattedEntitiesState, Option(overcatChoice)),
		new OvercattedYearMarkColumnOption().getColumns(overcattedEntitiesState),
		new FixedValueColumnOption().getColumns(overcattedEntitiesState, currentYearMark.right.toOption)
	).flatten

	lazy val optionsColumnCategories: Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]] =
		optionsColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

	lazy val optionsColumnValues: Map[ChosenYearExamGridColumn, Map[ExamGridEntity, ExamGridColumnValue]] = optionsColumns.map(c => c -> c.values).toMap

	lazy val perYearColumns: Map[StudentCourseYearDetails.YearOfStudy, Seq[PerYearExamGridColumn]] = Seq(
		new CoreModulesColumnOption().getColumns(overcattedEntitiesState),
		new CoreRequiredModulesColumnOption().getColumns(overcattedEntitiesState),
		new CoreOptionalModulesColumnOption().getColumns(overcattedEntitiesState),
		new OptionalModulesColumnOption().getColumns(overcattedEntitiesState)
	).flatMap(_.toSeq).groupBy { case (year, _) => year}.mapValues(_.flatMap { case (_, columns) => columns })

	lazy val perYearColumnValues: Map[PerYearExamGridColumn, Map[ExamGridEntity, Map[YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]] =
		perYearColumns.values.flatten.toSeq.map(c => c -> c.values).toMap

	lazy val perYearColumnCategories: Map[YearOfStudy, Map[String, Seq[PerYearExamGridColumn with HasExamGridColumnCategory]]] =
		perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category))

}

class ChooseOvercatColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "chooseovercat"

	override val label: String = ""

	override val sortOrder: Int = 0

	case class Column(state: ExamGridColumnState, selectedEntityId: Option[String]) extends ChosenYearExamGridColumn(state) {

		override val title: String = ""

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Spacer

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity -> {
				val entityId = GenerateExamGridOvercatCommand.overcatIdentifier(entity.validYears(state.yearOfStudy).moduleRegistrations)
				ExamGridColumnValueStringHtmlOnly(
					"<input type=\"radio\" name=\"overcatChoice\" value=\"%s\" %s />".format(
						entityId,
						if (selectedEntityId.contains(entityId)) "checked" else ""
					)
				)
			}).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = throw new UnsupportedOperationException

	def getColumns(state: ExamGridColumnState, selectedEntityId: Option[String]): Seq[ChosenYearExamGridColumn] = Seq(Column(state, selectedEntityId))

}

class FixedValueColumnOption extends ChosenYearExamGridColumnOption {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val label: String = ""

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CurrentYear

	override val mandatory = true

	case class Column(state: ExamGridColumnState, value: Option[BigDecimal]) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Weighted mean year mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity => entity -> (value match {
				case Some(mark) => ExamGridColumnValueDecimal(mark)
				case _ => ExamGridColumnValueString("")
			})).toMap
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = throw new UnsupportedOperationException

	def getColumns(state: ExamGridColumnState, value: Option[BigDecimal]): Seq[ChosenYearExamGridColumn] = Seq(Column(state, value))

}

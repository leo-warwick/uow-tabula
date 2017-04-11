package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindException, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreRequiredModulesColumnOption, ModuleReportsColumnOption}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

object GenerateExamGridMappingParameters {
	final val selectCourse = "selectCourse"
	final val usePreviousSettings = "usePreviousSettings"
	final val gridOptions = "gridOptions"
	final val coreRequiredModules = "coreRequiredModules"
	final val previewAndDownload = "previewAndDownload"
	final val excel = "excel"
	final val marksRecord = "marksRecord"
	final val marksRecordConfidential = "marksRecordConfidential"
	final val passList = "passList"
	final val passListConfidential = "passListConfidential"
	final val transcript = "transcript"
	final val transcriptConfidential = "transcriptConfidential"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent
	with ExamGridDocumentsController
	with TaskBenchmarking {

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Seq[ExamGridColumnOption], Seq[String])] with GenerateExamGridGridOptionsCommandRequest with PopulateOnForm
	type CoreRequiredModulesCommand = Appliable[Map[Route, Seq[CoreRequiredModule]]] with PopulateGenerateExamGridSetCoreRequiredModulesCommand
	type CheckOvercatCommand = Appliable[GenerateExamGridCheckAndApplyOvercatCommand.Result] with GenerateExamGridCheckAndApplyOvercatCommandState with SelfValidating

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(department, year)): _*)

	@ModelAttribute("selectCourseCommand")
	def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand(@PathVariable department: Department) = GenerateExamGridGridOptionsCommand(mandatory(department))

	@ModelAttribute("coreRequiredModulesCommand")
	def coreRequiredModulesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSetCoreRequiredModulesCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("checkOvercatCommmand")
	def checkOvercatCommmand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridCheckAndApplyOvercatCommand(department, academicYear, user)

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("coreRequiredModuleLookup")
	def coreRequiredModuleLookup(
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value = "yearOfStudy", required = false) yearOfStudy: JInteger
	): CoreRequiredModuleLookup = {
		if (Option(yearOfStudy).nonEmpty) {
			new CoreRequiredModuleLookup(academicYear, yearOfStudy, moduleRegistrationService)
		} else {
			null
		}
	}

	@ModelAttribute("ExamGridColumnValueType")
	def examGridColumnValueType = ExamGridColumnValueType

	@RequestMapping(method = Array(GET, POST))
	def selectCourseRender(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		gridOptionsCommand.populate()
		commonCrumbs(
			Mav("exams/grids/generate/selectCourse"),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.selectCourse))
	def selectCourseSubmit(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: JMap[String, String]
	): Mav = {
		selectCourseSubmit(
			selectCourseCommand,
			errors,
			gridOptionsCommand,
			checkOvercatCommmand,
			coreRequiredModuleLookup,
			department,
			academicYear,
			usePreviousSettings = false
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.usePreviousSettings))
	def selectCourseSubmitWithPreviousSettings(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam allRequestParams: JMap[String, String]
	): Mav = {
		selectCourseSubmit(
			selectCourseCommand,
			errors,
			gridOptionsCommand,
			checkOvercatCommmand,
			coreRequiredModuleLookup,
			department,
			academicYear,
			usePreviousSettings = true
		)
	}

	private def selectCourseSubmit(
		selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommmand: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup,
		department: Department,
		academicYear: AcademicYear,
		usePreviousSettings: Boolean
	): Mav = {
		if (errors.hasErrors) {
			selectCourseRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
		} else {
			val students = selectCourseCommand.apply()
			if (students.isEmpty) {
				errors.reject("examGrid.noStudents")
				selectCourseRender(selectCourseCommand, gridOptionsCommand, department, academicYear)
			} else {
				val jobInstance = jobService.add(Some(user), ImportMembersJob(students.map(_.universityId)))

				if (usePreviousSettings) {
					checkJobProgress(jobInstance.id, selectCourseCommand, gridOptionsCommand, checkOvercatCommmand, coreRequiredModuleLookup, department, academicYear)
				} else {
					gridOptionsRender(jobInstance.id, selectCourseCommand, department, academicYear)
				}
			}
		}
	}

	private def gridOptionsRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(Mav("exams/grids/generate/gridOption", "jobId" -> jobId), department, academicYear)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.gridOptions))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val columnIDs = gridOptionsCommand.apply()._1.map(_.identifier)
			if (columnIDs.contains(new CoreRequiredModulesColumnOption().identifier) || columnIDs.contains(new ModuleReportsColumnOption().identifier)) {
				coreRequiredModulesCommand.populate()
				coreRequiredModulesRender(jobId, department, academicYear)
			} else {
				checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, checkOvercatCommmand, coreRequiredModuleLookup, department, academicYear)
			}
		}
	}

	private def coreRequiredModulesRender(jobId: String, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/coreRequiredModules", "jobId" -> jobId),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.coreRequiredModules))
	def coreRequiredModules(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@Valid @ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			coreRequiredModulesRender(jobId, department, academicYear)
		} else {
			coreRequiredModulesCommand.apply()
			checkJobProgress(jobId, selectCourseCommand, gridOptionsCommand, checkOvercatCommmand, coreRequiredModuleLookup, department, academicYear)
		}
	}

	private def checkJobProgress(
		jobId: String,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommmand: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup,
		department: Department,
		academicYear: AcademicYear
	) = {
		val jobInstance = jobService.getInstance(jobId)
		if (jobInstance.isDefined && !jobInstance.get.finished) {
			val studentLastImportDates = selectCourseCommand.apply().map(e =>
				(Seq(e.firstName, e.lastName).mkString(" "), e.lastImportDate.getOrElse(new DateTime(0)))
			).sortBy(_._2)
			commonCrumbs(
				Mav("exams/grids/generate/jobProgress",
					"jobId" -> jobId,
					"jobProgress" -> jobInstance.get.progress,
					"jobStatus" -> jobInstance.get.status,
					"studentLastImportDates" -> studentLastImportDates
				),
				department,
				academicYear
			)
		} else {
			previewAndDownloadRender(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommmand,
				coreRequiredModuleLookup,
				department,
				academicYear,
				jobId
			)
		}
	}

	@RequestMapping(method = Array(POST), value = Array("/progress"))
	def jobProgress(@RequestParam jobId: String): Mav = {
		jobService.getInstance(jobId).map(jobInstance =>
			Mav(new JSONView(Map(
				"id" -> jobInstance.id,
				"status" -> jobInstance.status,
				"progress" -> jobInstance.progress,
				"finished" -> jobInstance.finished
			))).noLayout()
		).getOrElse(throw new ItemNotFoundException())
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.previewAndDownload))
	def previewAndDownload(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam jobId: String
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		previewAndDownloadRender(
			selectCourseCommand,
			gridOptionsCommand,
			checkOvercatCommmand,
			coreRequiredModuleLookup,
			department,
			academicYear,
			jobId
		)
	}

	private def previewAndDownloadRender(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommmand: CheckOvercatCommand,
		coreRequiredModules: CoreRequiredModuleLookup,
		department: Department,
		academicYear: AcademicYear,
		jobId: String
	): Mav = {
		val GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, routeRules) = benchmarkTask("GridData") {
			checkAndApplyOvercatAndGetGridData(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommmand,
				coreRequiredModules
			)
		}

		val oldestImport = benchmarkTask("oldestImport") { entities.flatMap(_.lastImportDate).reduceOption((a, b) => if(a.isBefore(b)) a else b) }
		val chosenYearColumnValues = benchmarkTask("chosenYearColumnValues") { Seq(studentInformationColumns, summaryColumns).flatten.map(c => c -> c.values).toMap }
		val chosenYearColumnCategories = benchmarkTask("chosenYearColumnCategories") { summaryColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category) }
		val perYearColumnValues = benchmarkTask("perYearColumnValues") { perYearColumns.values.flatten.toSeq.map(c => c -> c.values).toMap }
		val perYearColumnCategories = benchmarkTask("perYearColumnCategories") { perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)) }

		commonCrumbs(
			Mav("exams/grids/generate/preview",
				"oldestImport" -> oldestImport,
				"studentInformationColumns" -> studentInformationColumns,
				"perYearColumns" -> perYearColumns,
				"summaryColumns" -> summaryColumns,
				"chosenYearColumnValues" -> chosenYearColumnValues,
				"perYearColumnValues" -> perYearColumnValues,
				"chosenYearColumnCategories" -> chosenYearColumnCategories,
				"perYearColumnCategories" -> perYearColumnCategories,
				"entities" -> entities,
				"generatedDate" -> DateTime.now,
				"weightings" -> weightings,
				"normalLoadLookup" -> normalLoadLookup,
				"routeRules" -> routeRules,
				"jobId" -> jobId
			),
			department,
			academicYear
		)
	}

	protected case class GridData(
		entities: Seq[ExamGridEntity],
		studentInformationColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[YearOfStudy, Seq[PerYearExamGridColumn]],
		summaryColumns: Seq[ChosenYearExamGridColumn],
		weightings: Seq[CourseYearWeighting],
		normalLoadLookup: NormalLoadLookup,
		routeRulesLookup: UpstreamRouteRuleLookup
	)

	protected def checkAndApplyOvercatAndGetGridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCmd: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup
	): GridData = {

		checkOvercatCmd.selectCourseCommand = selectCourseCommand
		checkOvercatCmd.yearsToShow = gridOptionsCommand.yearsToShow
		val checkOvercatCommmandErrors = new BindException(selectCourseCommand, "checkOvercatCommand")
		checkOvercatCmd.validate(checkOvercatCommmandErrors)

		val entities = {
			if (checkOvercatCommmandErrors.hasErrors) {
				checkOvercatCmd.entities
			} else {
				checkOvercatCmd.apply().entities
			}
		}

		val normalLoadLookup = checkOvercatCmd.normalLoadLookup
		val routeRulesLookup = checkOvercatCmd.routeRulesLookup

		val (predefinedColumnOptions, customColumnTitles) = gridOptionsCommand.apply()

		val state = ExamGridColumnState(
			entities = entities,
			overcatSubsets = entities.flatMap(_.years.get(selectCourseCommand.yearOfStudy)).map(entityYear => entityYear ->
				moduleRegistrationService.overcattedModuleSubsets(entityYear, entityYear.markOverrides.getOrElse(Map()), normalLoadLookup(entityYear.route), routeRulesLookup(entityYear.route))
			).toMap,
			coreRequiredModuleLookup = coreRequiredModuleLookup,
			normalLoadLookup = normalLoadLookup,
			routeRulesLookup = routeRulesLookup,
			academicYear = selectCourseCommand.academicYear,
			yearOfStudy = selectCourseCommand.yearOfStudy,
			showFullName = gridOptionsCommand.showFullName,
			showComponentMarks = gridOptionsCommand.showComponentMarks,
			showModuleNames = gridOptionsCommand.showModuleNames
		)

		val studentInformationColumns = predefinedColumnOptions.collect { case c: StudentExamGridColumnOption => c }.flatMap(_.getColumns(state))
		val summaryColumns = predefinedColumnOptions.collect { case c: ChosenYearExamGridColumnOption => c }.flatMap(_.getColumns(state)) ++
			customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val perYearColumns = predefinedColumnOptions.collect { case c: PerYearExamGridColumnOption => c }
			.flatMap(_.getColumns(state).toSeq)
			.groupBy { case (year, _) => year}
			.mapValues(_.flatMap { case (_, columns) => columns })

		val weightings = (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
			courseAndRouteService.getCourseYearWeighting(selectCourseCommand.course.code, selectCourseCommand.academicYear, year)
		).sorted

		GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, routeRulesLookup)
	}

}

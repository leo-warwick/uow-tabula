package uk.ac.warwick.tabula.exams.grids.data

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.FilterStudentsOrRelationships
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridAuditCommand
import uk.ac.warwick.tabula.data.model.{CoreRequiredModuleLookup, NullCoreRequiredModuleLookup}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.modules.CoreRequiredModulesColumnOption
import uk.ac.warwick.tabula.exams.grids.documents.ExamGridDocument._
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

trait GeneratesExamGridData extends CourseAndRouteServiceComponent with MaintenanceModeServiceComponent with ModuleRegistrationServiceComponent {
	protected def checkAndApplyOvercatAndGetGridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCmd: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup
	): GridData = {

		checkOvercatCmd.selectCourseCommand = selectCourseCommand
		val checkOvercatCommandErrors = new BindException(selectCourseCommand, "checkOvercatCommand")
		checkOvercatCmd.validate(checkOvercatCommandErrors)

		val entities = {
			if (checkOvercatCommandErrors.hasErrors || maintenanceModeService.enabled) {
				checkOvercatCmd.entities
			} else {
				checkOvercatCmd.apply().entities
			}
		}

		val normalLoadLookup = checkOvercatCmd.normalLoadLookup
		val routeRulesLookup = checkOvercatCmd.routeRulesLookup

		val (predefinedColumnOptions, customColumnTitles) = gridOptionsCommand.apply()

		val overcatSubsets = entities
			.flatMap(entity => {
				entity.validYears.get(selectCourseCommand.studyYearByLevelOrBlock).map((entity, _))
			})
			.map { case (entity, entityYear) =>
				entityYear -> moduleRegistrationService.overcattedModuleSubsets(
					entityYear,
					entityYear.markOverrides.getOrElse(Map()),
					normalLoadLookup(entityYear.route),
					routeRulesLookup(entityYear.route, entityYear.level)
				)
			}.toMap

		val coreRequiredModulesColumnSelected = predefinedColumnOptions.exists(_.isInstanceOf[CoreRequiredModulesColumnOption])

		val state = ExamGridColumnState(
			entities = entities,
			overcatSubsets = overcatSubsets,
			coreRequiredModuleLookup = if (coreRequiredModulesColumnSelected) coreRequiredModuleLookup else NullCoreRequiredModuleLookup,
			normalLoadLookup = normalLoadLookup,
			routeRulesLookup = routeRulesLookup,
			academicYear = selectCourseCommand.academicYear,
			yearOfStudy = selectCourseCommand.studyYearByLevelOrBlock,
			department = selectCourseCommand.department,
			nameToShow = gridOptionsCommand.nameToShow,
			showComponentMarks = gridOptionsCommand.showComponentMarks,
			showZeroWeightedComponents = gridOptionsCommand.showZeroWeightedComponents,
			showComponentSequence = gridOptionsCommand.showComponentSequence,
			showModuleNames = gridOptionsCommand.moduleNameToShow,
			calculateYearMarks = gridOptionsCommand.calculateYearMarks,
			isLevelGrid = selectCourseCommand.isLevelGrid
		)

		val studentInformationColumns = predefinedColumnOptions.collect { case c: StudentExamGridColumnOption => c }.flatMap(_.getColumns(state))
		val summaryColumns = predefinedColumnOptions.collect { case c: ChosenYearExamGridColumnOption => c }.flatMap(_.getColumns(state)) ++
			customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val selectedYears = selectCourseCommand.courseYearsToShow.asScala.map(_.stripPrefix("Year").toInt).toSeq
		val perYearColumns = predefinedColumnOptions.collect { case c: PerYearExamGridColumnOption => c }
			.flatMap(_.getColumns(state).toSeq)
			.filter { case (year, _) => selectedYears.contains(year) }
			.groupBy { case (year, _) => year }
			.mapValues(_.flatMap { case (_, columns) => columns })

		val weightings = ListMap(selectCourseCommand.courses.asScala.map(course => {
			course -> (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
				courseAndRouteService.getCourseYearWeighting(course.code, selectCourseCommand.academicYear, year)
			).sorted
		}).sortBy { case (course, _) => course.code }: _*)

		GenerateExamGridAuditCommand(selectCourseCommand).apply()

		GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, routeRulesLookup)
	}

}

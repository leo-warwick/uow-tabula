package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import org.springframework.web.servlet.View
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridMarksRecordExporter, ExamGridPassListExporter, ExamGridTranscriptExporter, GenerateExamGridExporter}
import uk.ac.warwick.tabula.data.model.{CoreRequiredModuleLookup, Department, UpstreamRouteRuleLookup}
import uk.ac.warwick.tabula.services.AutowiringProgressionServiceComponent
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalLoadLookup}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.{ExcelView, WordView}

import scala.collection.JavaConverters._

trait ExamGridDocumentsController extends ExamsController
	with AutowiringUpstreamRouteRuleServiceComponent
	with AutowiringProgressionServiceComponent
	with AutowiringNormalCATSLoadServiceComponent {

	self: GenerateExamGridController =>

	var messageSource: MessageSource = Wire.auto[MessageSource]

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.excel))
	def excel(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): View = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}

		val GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadLookup, _) = benchmarkTask("GridData") { checkAndApplyOvercatAndGetGridData(
			selectCourseCommand,
			gridOptionsCommand,
			checkOvercatCommmand,
			coreRequiredModuleLookup
		)}

		val chosenYearColumnValues = benchmarkTask("chosenYearColumnValues") { Seq(studentInformationColumns, summaryColumns).flatten.map(c => c -> c.values).toMap }
		val perYearColumnValues = benchmarkTask("perYearColumnValues") { perYearColumns.values.flatten.toSeq.map(c => c -> c.values).toMap }

		new ExcelView(
			"Exam grid for %s %s %s %s.xlsx".format(
				department.name,
				selectCourseCommand.course.code,
				selectCourseCommand.routes.size match {
					case 0 => "All routes"
					case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
					case n => s"$n routes"
				},
				academicYear.toString.replace("/","-")
			),
			GenerateExamGridExporter(
				department = department,
				academicYear = academicYear,
				course = selectCourseCommand.course,
				routes = selectCourseCommand.routes.asScala,
				yearOfStudy = selectCourseCommand.yearOfStudy,
				yearWeightings = weightings,
				normalLoadLookup = normalLoadLookup,
				entities = entities,
				leftColumns = studentInformationColumns,
				perYearColumns = perYearColumns,
				rightColumns = summaryColumns,
				chosenYearColumnValues = chosenYearColumnValues,
				perYearColumnValues = perYearColumnValues,
				showComponentMarks = gridOptionsCommand.showComponentMarks
			)
		)
	}

	private def marksRecordRender(selectCourseCommand: SelectCourseCommand, isConfidential: Boolean): View = {
		val entities = selectCourseCommand.apply()
		new WordView(
			"%sMarks record for %s %s %s %s.docx".format(
				if (isConfidential) "Confidential " else "",
				selectCourseCommand.department.name,
				selectCourseCommand.course.code,
				selectCourseCommand.routes.size match {
					case 0 => "All routes"
					case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
					case n => s"$n routes"
				},
				selectCourseCommand.academicYear.toString.replace("/","-")
			),
			ExamGridMarksRecordExporter(
				entities,
				selectCourseCommand.course,
				progressionService,
				new NormalLoadLookup(selectCourseCommand.academicYear, selectCourseCommand.yearOfStudy, normalCATSLoadService),
				new UpstreamRouteRuleLookup(selectCourseCommand.academicYear, selectCourseCommand.yearOfStudy, upstreamRouteRuleService),
				isConfidential = isConfidential
			)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.marksRecord))
	def marksRecord(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			marksRecordRender(selectCourseCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.marksRecordConfidential))
	def marksRecordConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			marksRecordRender(selectCourseCommand, isConfidential = true)
		}
	}

	private def passListRender(selectCourseCommand: SelectCourseCommand, isConfidential: Boolean): View = {
		val entities = selectCourseCommand.apply()
		new WordView(
			"%sPass list for %s %s %s %s.docx".format(
				if (isConfidential) "Confidential " else "",
				selectCourseCommand.department.name,
				selectCourseCommand.course.code,
				selectCourseCommand.routes.size match {
					case 0 => "All routes"
					case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
					case n => s"$n routes"
				},
				selectCourseCommand.academicYear.toString.replace("/","-")
			),
			ExamGridPassListExporter(
				entities,
				selectCourseCommand.department,
				selectCourseCommand.course,
				selectCourseCommand.yearOfStudy,
				selectCourseCommand.academicYear,
				progressionService,
				new NormalLoadLookup(selectCourseCommand.academicYear, selectCourseCommand.yearOfStudy, normalCATSLoadService),
				new UpstreamRouteRuleLookup(selectCourseCommand.academicYear, selectCourseCommand.yearOfStudy, upstreamRouteRuleService),
				isConfidential
			)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.passList))
	def passList(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			passListRender(selectCourseCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.passListConfidential))
	def passListConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			passListRender(selectCourseCommand, isConfidential = true)
		}
	}

	private def transcriptRender(selectCourseCommand: SelectCourseCommand, isConfidential: Boolean): View = {
		val entities = selectCourseCommand.apply()
		new WordView(
			"%sTranscript for %s %s %s %s.docx".format(
				if (isConfidential) "Confidential " else "",
				selectCourseCommand.department.name,
				selectCourseCommand.course.code,
				selectCourseCommand.routes.size match {
					case 0 => "All routes"
					case 1 => selectCourseCommand.routes.get(0).code.toUpperCase
					case n => s"$n routes"
				},
				selectCourseCommand.academicYear.toString.replace("/","-")
			),
			ExamGridTranscriptExporter(
				entities,
				isConfidential = isConfidential
			)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.transcript))
	def transcript(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			transcriptRender(selectCourseCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.transcriptConfidential))
	def transcriptConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors
	): View = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			transcriptRender(selectCourseCommand, isConfidential = true)
		}
	}

}

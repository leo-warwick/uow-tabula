package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{CoreRequiredModuleLookup, Department}
import uk.ac.warwick.tabula.exams.grids.documents._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.jobs.exams.GenerateExamGridDocumentJob
import uk.ac.warwick.tabula.services.AutowiringProgressionServiceComponent
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

import scala.collection.JavaConverters._

trait ExamGridDocumentsController extends ExamsController
	with AutowiringUpstreamRouteRuleServiceComponent
	with AutowiringProgressionServiceComponent
	with AutowiringNormalCATSLoadServiceComponent {

	self: GenerateExamGridController =>

	var messageSource: MessageSource = Wire.auto[MessageSource]

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.excel))
	def mergedCells(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = excel(
		selectCourseCommand,
		selectCourseCommandErrors,
		gridOptionsCommand,
		gridOptionsCommandErrors,
		checkOvercatCommand,
		coreRequiredModuleLookup,
		department,
		academicYear,
		mergedCells = true
	)

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.excelNoMergedCells))
	def noMergedCells(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommand") checkOvercatCommand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModuleLookup") coreRequiredModuleLookup: CoreRequiredModuleLookup,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = excel(
		selectCourseCommand,
		selectCourseCommandErrors,
		gridOptionsCommand,
		gridOptionsCommandErrors,
		checkOvercatCommand,
		coreRequiredModuleLookup,
		department,
		academicYear,
		mergedCells = false
	)

	def excel(
		selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		checkOvercatCommand: CheckOvercatCommand,
		coreRequiredModuleLookup: CoreRequiredModuleLookup,
		department: Department,
		academicYear: AcademicYear,
		mergedCells: Boolean
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}

		createJobAndRedirect(ExcelGridDocument, ExcelGridDocument.options(mergedCells), department, academicYear, selectCourseCommand, gridOptionsCommand)
	}

	private def marksRecordRender(department: Department, academicYear: AcademicYear, selectCourseCommand: SelectCourseCommand, gridOptionsCommand: GridOptionsCommand, isConfidential: Boolean): Mav = {
		createJobAndRedirect(MarksRecordDocument, MarksRecordDocument.options(isConfidential), department, academicYear, selectCourseCommand, gridOptionsCommand)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.marksRecord))
	def marksRecord(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			marksRecordRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.marksRecordConfidential))
	def marksRecordConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			marksRecordRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = true)
		}
	}

	private def passListRender(department: Department, academicYear: AcademicYear, selectCourseCommand: SelectCourseCommand, gridOptionsCommand: GridOptionsCommand, isConfidential: Boolean): Mav = {
		createJobAndRedirect(PassListDocument, PassListDocument.options(isConfidential), department, academicYear, selectCourseCommand, gridOptionsCommand)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.passList))
	def passList(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			passListRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.passListConfidential))
	def passListConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			passListRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = true)
		}
	}

	private def transcriptRender(department: Department, academicYear: AcademicYear, selectCourseCommand: SelectCourseCommand, gridOptionsCommand: GridOptionsCommand, isConfidential: Boolean): Mav = {
		createJobAndRedirect(TranscriptDocument, TranscriptDocument.options(isConfidential), department, academicYear, selectCourseCommand, gridOptionsCommand)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.transcript))
	def transcript(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			transcriptRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = false)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.transcriptConfidential))
	def transcriptConfidential(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors) {
			throw new IllegalArgumentException(selectCourseCommandErrors.getAllErrors.asScala.map(e =>
				messageSource.getMessage(e.getCode, e.getArguments, null)).mkString(", ")
			)
		} else {
			transcriptRender(department, academicYear, selectCourseCommand, gridOptionsCommand, isConfidential = true)
		}
	}

	private def createJobAndRedirect(
		document: ExamGridDocumentPrototype,
		options: Map[String, Any] = Map.empty,
		department: Department,
		academicYear: AcademicYear,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand
	): Mav = {
		val job = jobService.add(Option(user), GenerateExamGridDocumentJob(
			document = document,
			options = options,
			department = department,
			academicYear = academicYear,
			selectCourseCommand = selectCourseCommand,
			gridOptionsCommand = gridOptionsCommand
		))

		Redirect(Routes.Grids.documentProgress(department, academicYear, job))
	}

}

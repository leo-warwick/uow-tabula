package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JHashMap, JMap}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.view._
import uk.ac.warwick.tabula.commands.attendance.{CSVAttendanceExtractor, CSVAttendanceExtractorInternal}
import uk.ac.warwick.tabula.commands.{Appliable, FiltersStudentsBase, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/points/{templatePoint}/record/upload"))
class RecordMonitoringPointUploadController extends AttendanceController {

	@ModelAttribute("extractor")
	def extractor = CSVAttendanceExtractor()

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
		RecordMonitoringPointCommand(mandatory(department), mandatory(academicYear), mandatory(templatePoint), user)

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): Mav = {
		Mav("attendance/upload_attendance",
			"uploadUrl" -> Routes.View.pointRecordUpload(department, academicYear, templatePoint, filterCommand.serializeFilter),
			"ajax" -> ajax
		).crumbs(
			Breadcrumbs.View.HomeForYear(academicYear),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Points(department, academicYear)
		).noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("extractor") extractor: CSVAttendanceExtractorInternal,
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult] with FiltersStudentsBase,
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]]
			with SetFilterPointsResultOnRecordMonitoringPointCommand with SelfValidating
			with PopulateOnForm with RecordMonitoringPointCommandState,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	): Mav = {
		val attendance = extractor.extract(errors)
		if (errors.hasErrors) {
			form(filterCommand, department, academicYear, templatePoint)
		} else {
			val filterResult = filterCommand.apply()
			cmd.setFilteredPoints(filterResult)
			cmd.populate()
			val newCheckpointMap: JMap[StudentMember, JMap[AttendanceMonitoringPoint, AttendanceState]] =
				JHashMap(cmd.checkpointMap.asScala.map { case (student, pointMap) =>
					student -> JHashMap(pointMap.asScala.map { case (point, oldState) =>
						point -> (attendance.getOrElse(student, oldState) match {
							case state: AttendanceState if state == AttendanceState.NotRecorded => null
							case state => state
						})
					}.toMap)
				}.toMap)
			cmd.checkpointMap = newCheckpointMap
			cmd.validate(errors)
			if (errors.hasErrors) {
				form(filterCommand, department, academicYear, templatePoint)
			} else {
				cmd.apply()
				Redirect(Routes.View.points(department, academicYear))
			}
		}
	}

}
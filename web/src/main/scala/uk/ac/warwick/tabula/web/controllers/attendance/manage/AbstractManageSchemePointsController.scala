package uk.ac.warwick.tabula.web.controllers.attendance.manage

import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.manage.{FindPointsCommandState, FindPointsCommand, FindPointsResult}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.web.Mav
import collection.JavaConverters._

abstract class AbstractManageSchemePointsController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(mandatory(department), mandatory(academicYear), None)

	protected def render(
		findCommandResult: FindPointsResult,
		scheme: AttendanceMonitoringScheme,
		points: JInteger,
		actionCompleted: String
	): Mav

	@RequestMapping
	def home(
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult] with FindPointsCommandState,
		@PathVariable scheme: AttendanceMonitoringScheme,
		@RequestParam(required = false) points: JInteger,
		@RequestParam(required = false) actionCompleted: String
	): Mav = {
		findCommand.findSchemes = Seq(scheme).asJava
		render(findCommand.apply(), scheme, points, actionCompleted)
	}

}

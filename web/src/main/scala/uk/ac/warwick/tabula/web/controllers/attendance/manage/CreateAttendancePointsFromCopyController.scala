package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointType, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/addpoints/copy"))
class CreateAttendancePointsFromCopyController extends AttendanceController with HasMonthNames {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _

	@ModelAttribute("allAcademicYears")
	def allAcademicYears(@PathVariable academicYear: AcademicYear): Seq[AcademicYear] = {
		Seq(academicYear.previous, academicYear, academicYear.next)
	}

	@ModelAttribute("allDepartments")
	def allDepartments(@PathVariable department: Department): Seq[Department] = {
		(Seq(mandatory(department)) ++ moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage))
			.flatMap { dept =>
				Seq(dept) ++ dept.children.asScala
			}
			.sortBy(_.code).distinct
	}

	@ModelAttribute("searchCommand")
	def searchCommand(@RequestParam(required = false) searchDepartment: Department, @RequestParam(required = false) searchAcademicYear: AcademicYear): CreateNewAttendancePointsFromCopySearchCommandInternal with ComposableCommand[Seq[AttendanceMonitoringScheme]] with AutowiringAttendanceMonitoringServiceComponent with CreateNewAttendancePointsFromCopySearchPermissions with CreateNewAttendancePointsFromCopySearchCommandState with ReadOnly with Unaudited = {
		if (searchDepartment == null || searchAcademicYear == null)
			null
		else
			CreateNewAttendancePointsFromCopySearchCommand(searchDepartment, searchAcademicYear)
	}

	@ModelAttribute("findCommand")
	def findCommand(
		@RequestParam(required = false) searchDepartment: Department,
		@RequestParam(required = false) searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	): FindPointsCommandInternal with ComposableCommand[FindPointsResult] with AutowiringTermServiceComponent with AutowiringAttendanceMonitoringServiceComponent with GroupsPoints with FindPointsPermissions with FindPointsCommandState with ReadOnly with Unaudited = {
		if (searchDepartment == null || searchAcademicYear == null)
			null
		else
			FindPointsCommand(searchDepartment, searchAcademicYear, Option(schemes.asScala.head.pointStyle))
	}

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) =
		CreateNewAttendancePointsFromCopyCommand(department, academicYear, schemes.asScala)

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("attendance/manage/copypoints",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("search"))
	def search(
		@ModelAttribute("searchCommand") searchCommand: Appliable[Seq[AttendanceMonitoringScheme]],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam searchDepartment: Department,
		@RequestParam searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	): Mav = {
		val allSchemes = searchCommand.apply()
		Mav("attendance/manage/copypoints",
			"searchDepartment" -> searchDepartment,
			"searchAcademicYear" -> searchAcademicYear,
			"allSchemes" -> allSchemes,
			"allTypes" -> AttendanceMonitoringPointType.values,
			"findResult" -> findCommand.apply(),
			"monthNames" -> monthNames(searchAcademicYear),
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.HomeForYear(academicYear),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("copy"))
	def copy(
		@ModelAttribute("searchCommand") searchCommand: Appliable[Seq[AttendanceMonitoringScheme]],
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with SetsFindPointsResultOnCommandState with SelfValidating,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam searchDepartment: Department,
		@RequestParam searchAcademicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	): Mav = {
		val allSchemes = searchCommand.apply()
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		cmd.validate(errors)
		if (errors.hasErrors) {
			Mav("attendance/manage/copypoints",
				"searchDepartment" -> searchDepartment,
				"searchAcademicYear" -> searchAcademicYear,
				"findResult" -> findCommandResult,
				"allSchemes" -> allSchemes,
				"allTypes" -> AttendanceMonitoringPointType.values,
				"errors" -> errors,
				"monthNames" -> monthNames(searchAcademicYear),
				"returnTo" -> getReturnTo("")
			).crumbs(
				Breadcrumbs.Manage.HomeForYear(academicYear),
				Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(""),
				"points" -> points.size.toString,
				"schemes" -> points.map(_.scheme.id).mkString(",")
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@RequestParam schemes: JList[AttendanceMonitoringScheme]): Mav = {
		Redirect(
			getReturnTo(""),
			"schemes" -> schemes.asScala.map(_.id).mkString(",")
		)
	}

}

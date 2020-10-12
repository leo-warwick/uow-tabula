package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/addpoints/template"))
class CreateAttendancePointsFromTemplateController extends AttendanceController {

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): AddTemplatePointsToSchemesCommandInternal with ComposableCommand[Seq[AttendanceMonitoringPoint]] with AddTemplatePointsToSchemesCommandState with AddTemplatePointsToSchemesPermissions with AutowiringAttendanceMonitoringServiceComponent with AutowiringProfileServiceComponent with AddTemplatePointsToSchemesDescription with AddTemplatePointsToSchemesValidation = {
    AddTemplatePointsToSchemesCommand(mandatory(department), mandatory(academicYear))
  }

  @RequestMapping(method = Array(POST))
  def post(
    @ModelAttribute("command") cmd: AddTemplatePointsToSchemesCommandState,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    Mav("attendance/manage/templates",
      "schemes" -> cmd.schemes,
      "templates" -> cmd.templateSchemeItems(),
      "returnTo" -> getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear))
    ).crumbs(
      Breadcrumbs.Manage.HomeForYear(academicYear),
      Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
    )
  }

  @RequestMapping(method = Array(POST), params = Array("templateScheme"))
  def submit(
    @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with AddTemplatePointsToSchemesCommandState with SelfValidating,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
    cmd.validate(errors)
    if (errors.hasErrors) {
      Mav("attendance/manage/templates",
        "schemes" -> cmd.schemes,
        "templates" -> cmd.templateSchemeItems(),
        "returnTo" -> getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear)),
        "errors" -> errors
      ).crumbs(
        Breadcrumbs.Manage.HomeForYear(academicYear),
        Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
      )
    } else {
      val points = cmd.apply()
      Redirect(getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear)),
        "points" -> points.size.toString,
        "schemes" -> points.map(_.scheme.id).mkString(",")
      )
    }

  }

}

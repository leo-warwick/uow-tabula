package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.{MarksDepartmentHomeCommand, MarksManagementHomeCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}

abstract class AbstractMarksAdminDepartmentHomepageController
  extends BaseController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringUserSettingsServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  override val departmentPermission: Permission = MarksManagementHomeCommand.AdminPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]): MarksDepartmentHomeCommand.Command =
    MarksDepartmentHomeCommand(department, academicYear.getOrElse(AcademicYear.now()), user)

  @RequestMapping
  def list(@ModelAttribute("command") command: MarksDepartmentHomeCommand.Command, @PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
    val academicYear = activeAcademicYear.getOrElse(AcademicYear.now())
    val results = command.apply()
    val hasMultipleOccurrences = results.modules.groupBy(_.moduleCode).values.map(_.map(_.occurrence).distinct).exists(_.size > 1)

    Mav("marks/admin/module-occurrences",
      "results" -> results.modules,
      "hasMultipleOccurrences" -> hasMultipleOccurrences,
      "academicYear" -> academicYear,
      "pendingComponentMarkChanges" -> results.pendingComponentMarkChanges,
      "pendingModuleMarkChanges" -> results.pendingModuleMarkChanges,
    ).crumbs(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)
    ).secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)): _*)
  }

}

@Controller
@RequestMapping(Array("/marks/admin/{department}"))
class MarksAdminDepartmentHomepageController extends AbstractMarksAdminDepartmentHomepageController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}"))
class MarksAdminDepartmentHomepageForYearController extends AbstractMarksAdminDepartmentHomepageController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

}

/**
  * Redirects to handle these otherwise nonexistent parent paths.
  */
@Controller
@RequestMapping(Array("/marks/admin"))
class MarksAdminHomeController extends BaseController {

  @RequestMapping
  def redirectHome: Mav = Redirect(Routes.marks.home)

}

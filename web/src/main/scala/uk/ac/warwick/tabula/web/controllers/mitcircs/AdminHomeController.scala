package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.mitcircs.{AdminHomeCommand, ListMitCircsPanelsCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}

abstract class AbstractAdminDeptController
  extends BaseController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  hideDeletedItems

  override val departmentPermission: Permission = AdminHomeCommand.RequiredPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): AdminHomeCommand.Command =
    AdminHomeCommand(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.now()))

  @ModelAttribute("allSubmissionStates")
  def allSubmissionStates: Seq[MitigatingCircumstancesSubmissionState] =
    MitigatingCircumstancesSubmissionState.values.filterNot(_ == MitigatingCircumstancesSubmissionState.Draft)

  @ModelAttribute("listPanelsCommand")
  def listPanelsCommand(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): ListMitCircsPanelsCommand.Command =
    ListMitCircsPanelsCommand(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.now()))

  @RequestMapping(params = Array("!ajax"), headers = Array("!X-Requested-With"))
  def home(@ModelAttribute("listPanelsCommand") listPanelsCommand: ListMitCircsPanelsCommand.Command, @ModelAttribute("command") command: AdminHomeCommand.Command, errors: Errors, @PathVariable department: Department): Mav =
    Mav("mitcircs/admin/admin-home",
      "academicYear" -> command.year,
      "panels" -> listPanelsCommand.apply())
      .crumbs(MitCircsBreadcrumbs.Admin.HomeForYear(department, command.year, active = true))
      .secondCrumbs(academicYearBreadcrumbs(command.year)(Routes.Admin.home(department, _)): _*)

  @RequestMapping
  def results(@ModelAttribute("command") command: AdminHomeCommand.Command, errors: Errors): Mav = {
    val info = command.apply()
    Mav("mitcircs/admin/submissions", "submissions" -> info.submissions).noLayout()
  }

}

@Controller
@RequestMapping(Array("/mitcircs/admin/{department}"))
class AdminMitCircsDepartmentHomeController extends AbstractAdminDeptController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/mitcircs/admin/{department}/{academicYear:\\d{4}}"))
class AdminMitCircsDepartmentHomeForYearController extends AbstractAdminDeptController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

}

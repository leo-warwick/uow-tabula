package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/assessment-components"))
class ListAssessmentComponentsController
  extends BaseController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringUserSettingsServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  override val departmentPermission: Permission = ListAssessmentComponentsCommand.AdminPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): ListAssessmentComponentsCommand.Command =
    ListAssessmentComponentsCommand(mandatory(department), mandatory(academicYear), user)

  @RequestMapping
  def list(@ModelAttribute("command") command: ListAssessmentComponentsCommand.Command, @PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav =
    Mav("marks/admin/assessment-components/list", "results" -> command.apply())
      .crumbs(
        MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
        MarksBreadcrumbs.Admin.AssessmentComponents(department, academicYear, active = true)
      ).secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.AssessmentComponents(department, _)): _*)

}

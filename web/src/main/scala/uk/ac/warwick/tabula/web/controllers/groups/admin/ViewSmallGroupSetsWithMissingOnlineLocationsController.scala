package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.ViewSmallGroupSetsWithMissingOnlineLocationsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringSmallGroupServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/missing-online-locations"))
class ViewSmallGroupSetsWithMissingOnlineLocationsController extends GroupsController
  with DepartmentScopedController with AcademicYearScopedController
  with AutowiringSmallGroupServiceComponent
  with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  override val departmentPermission: Permission = ViewSmallGroupSetsWithMissingOnlineLocationsCommand.RequiredPermission

  @ModelAttribute("command")
  def command(@PathVariable academicYear: AcademicYear, @PathVariable department: Department): Appliable[ViewSmallGroupSetsWithMissingOnlineLocationsCommand.Result] = {
    ViewSmallGroupSetsWithMissingOnlineLocationsCommand(mandatory(academicYear), mandatory(department))
  }

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  @RequestMapping
  def view(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @ModelAttribute("command") command: Appliable[Seq[(SmallGroupSet, Seq[SmallGroupEvent])]]): Mav = {
    Mav("groups/admin/groups/missing-online-locations")
      .addObjects(
        "smallGroupSets" -> command.apply()
      )
      .crumbs(Breadcrumbs.Department(department, academicYear))
      .secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.groups.admin.missingOnlineLocations(department, year)): _*)
  }

}


package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.CopyAssignmentsCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

import scala.collection.JavaConverters._

abstract class AbstractCopyAssignmentsController extends CourseworkController
  with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  @ModelAttribute("academicYearChoices")
  def academicYearChoices: JList[AcademicYear] =
    AcademicYear.now().yearsSurrounding(0, 1).asJava

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/{module}/{academicYear:\\d{4}}/copy-assignments"))
class CopyModuleAssignmentsController extends AbstractCopyAssignmentsController with AliveAssignmentsMap {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

  @ModelAttribute("copyAssignmentsCommand")
  def copyAssignmentsCommand(@PathVariable module: Module, @PathVariable academicYear: AcademicYear): CopyAssignmentsCommand.Command =
    CopyAssignmentsCommand(mandatory(module), mandatory(academicYear))

  @RequestMapping
  def showForm(@PathVariable module: Module, @ModelAttribute("copyAssignmentsCommand") cmd: CopyAssignmentsCommand.Command): Mav =
    Mav("cm2/admin/modules/copy_assignments",
      "title" -> module.name,
      "cancel" -> Routes.admin.moduleWithinDepartment(module, cmd.academicYear),
      "department" -> module.adminDepartment,
      "map" -> moduleAssignmentMap(cmd.modules))
      .crumbsList(Breadcrumbs.module(module, cmd.academicYear))
      .secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(Routes.admin.module.copyAssignments(module, _)): _*)

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("copyAssignmentsCommand") cmd: CopyAssignmentsCommand.Command, @PathVariable module: Module): Mav = {
    cmd.apply()
    Redirect(Routes.admin.moduleWithinDepartment(module, cmd.academicYear))
  }

}

abstract class AbstractCopyDepartmentAssignmentsController extends AbstractCopyAssignmentsController with AliveAssignmentsMap
  with DepartmentScopedController
  with AutowiringModuleAndDepartmentServiceComponent {

  override val departmentPermission: Permission = CopyAssignmentsCommand.AdminPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("copyAssignmentsCommand")
  def copyAssignmentsCommand(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): CopyAssignmentsCommand.Command =
    CopyAssignmentsCommand(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.now()))

  @RequestMapping
  def showForm(@PathVariable department: Department, @ModelAttribute("copyAssignmentsCommand") cmd: CopyAssignmentsCommand.Command): Mav =
    Mav("cm2/admin/modules/copy_assignments",
      "academicYear" -> cmd.academicYear,
      "title" -> department.name,
      "cancel" -> Routes.admin.department(department),
      "map" -> moduleAssignmentMap(cmd.modules),
      "showSubHeadings" -> true)
      .crumbsList(Breadcrumbs.department(department, Some(cmd.academicYear)))
      .secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(Routes.admin.copyAssignments(department, _)): _*)

  @RequestMapping(method = Array(POST))
  def submit(@ModelAttribute("copyAssignmentsCommand") cmd: CopyAssignmentsCommand.Command, @PathVariable department: Department): Mav = {
    cmd.apply()
    Redirect(Routes.admin.department(department, cmd.academicYear))
  }

}

trait AliveAssignmentsMap {
  def moduleAssignmentMap(modules: Seq[Module]): Map[String, Seq[Assignment]] =
    modules.map { module => module.code -> module.assignments.asScala.filter(_.isAlive) }
      .toMap
      .filter { case (_, assignments) => assignments.nonEmpty }
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/department/{department}/copy-assignments"))
class CopyDepartmentAssignmentsController extends AbstractCopyDepartmentAssignmentsController {
  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)
}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/department/{department}/{academicYear:\\d{4}}/copy-assignments"))
class CopyDepartmentAssignmentsForYearController extends AbstractCopyDepartmentAssignmentsController {
  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))
}

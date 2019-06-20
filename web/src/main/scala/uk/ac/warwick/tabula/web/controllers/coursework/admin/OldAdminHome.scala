package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.{Mav, Routes => WebRoutes}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.cm2.web.{Routes => CM2Routes}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.xml.Elem

/**
  * Screens for department and module admins.
  */

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(Array("/${cm1.prefix}/admin", "/${cm1.prefix}/admin/department", "/${cm1.prefix}/admin/module"))
class OldCourseworkAdminHomeController extends OldCourseworkController with AutowiringFeaturesComponent {
  @RequestMapping(method = Array(GET, HEAD))
  def homeScreen(user: CurrentUser) = {
    if (features.redirectCM1) {
      Redirect(CM2Routes.home)
    } else {
      Redirect(Routes.home)
    }
  }
}

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(value = Array("/${cm1.prefix}/admin/department/{dept}"))
class OldCourseworkAdminDepartmentHomeController extends OldCourseworkController with AutowiringFeaturesComponent {

  hideDeletedItems

  @ModelAttribute def command(@PathVariable dept: Department, user: CurrentUser) =
    new AdminDepartmentHomeCommand(dept, user)

  @RequestMapping
  def adminDepartment(cmd: AdminDepartmentHomeCommand): Mav = {
    if (features.redirectCM1) {
      Redirect(CM2Routes.admin.department(cmd.department))
    } else {
      val info = cmd.apply()

      Mav("coursework/admin/department",
        "department" -> cmd.department,
        "modules" -> info.sortWith(_.code.toLowerCase < _.code.toLowerCase)
      )
    }
  }

  @RequestMapping(Array("/assignments.xml"))
  def xml(cmd: AdminDepartmentHomeCommand, @PathVariable dept: Department): Elem = {
    val info = cmd.apply()

    new AdminHomeExports.XMLBuilder(dept, DepartmentHomeInformation(info, cmd.gatherNotices(info))).toXML
  }
}

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(value = Array("/${cm1.prefix}/admin/module/{module}"))
class OldCourseworkAdminModuleHomeController extends OldCourseworkController with AutowiringFeaturesComponent {

  hideDeletedItems

  @ModelAttribute("command") def command(@PathVariable module: Module, user: CurrentUser) =
    new ViewViewableCommand(Permissions.Module.ManageAssignments, module)

  @RequestMapping
  def adminModule(@ModelAttribute("command") cmd: Appliable[Module]): Mav = {
    val module = cmd.apply()
    if (features.redirectCM1) {
      Redirect(WebRoutes.admin.module(module))
    } else {
      if (ajax) Mav("coursework/admin/modules/admin_partial").noLayout()
      else Mav("coursework/admin/modules/admin").crumbs(Breadcrumbs.Department(module.adminDepartment))
    }
  }
}

class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[Seq[Module]]
  with ReadOnly with Unaudited {

  var securityService: SecurityService = Wire.auto[SecurityService]
  var moduleService: ModuleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

  val modules: JList[Module] =
    if (securityService.can(user, Permissions.Module.ManageAssignments, mandatory(department))) {
      // This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
      PermissionCheck(Permissions.Module.ManageAssignments, department)

      department.modules
    } else {
      val managedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments, department).toList

      // This is implied by the above, but it's nice to check anyway
      PermissionCheckAll(Permissions.Module.ManageAssignments, managedModules)

      if (managedModules.isEmpty)
        throw PermissionDeniedException(user, Permissions.Module.ManageAssignments, department)

      managedModules.asJava
    }

  def applyInternal(): mutable.Buffer[Module] = {
    modules.asScala.sortBy { module => (module.assignments.isEmpty, module.code) }
  }

  def gatherNotices(modules: Seq[Module]): Map[String, Seq[Assignment]] = benchmarkTask("Gather notices") {
    val unpublished = for (
      module <- modules;
      assignment <- module.assignments.asScala
      if assignment.isAlive && assignment.hasUnreleasedFeedback
    ) yield assignment

    Map(
      "unpublishedAssignments" -> unpublished
    )
  }

}

case class DepartmentHomeInformation(modules: Seq[Module], notices: Map[String, Seq[Assignment]])
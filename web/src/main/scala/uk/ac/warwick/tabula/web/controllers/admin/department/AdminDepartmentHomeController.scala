package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.admin.department.AdminDepartmentHomeCommand
import uk.ac.warwick.tabula.data.model.{Department, Module, Route}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.{AdminController, AdminDepartmentsModulesAndRoutes}

import scala.collection.JavaConverters._

@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends AdminController with DepartmentScopedController with AutowiringUserSettingsServiceComponent
	with AdminDepartmentsModulesAndRoutes with AutowiringModuleAndDepartmentServiceComponent with AutowiringCourseAndRouteServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	type AdminDepartmentHomeCommand = Appliable[(Seq[Module], Seq[Route])]

	override val departmentPermission: Permission = null

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("departmentsWithPermission")
	override def departmentsWithPermission: Seq[Department] = {
		def withSubDepartments(d: Department) = Seq(d) ++ d.children.asScala.toSeq.sortBy(_.fullName)

		val result = ownedDepartmentsModulesAndRoutes(user)
		(result.departments ++ result.modules.map(_.adminDepartment) ++ result.routes.map(_.adminDepartment))
			.toSeq.sortBy(_.fullName).flatMap(withSubDepartments).distinct
	}

	@ModelAttribute def command(@PathVariable("department") dept: Department, user: CurrentUser): AdminDepartmentHomeCommand =
		AdminDepartmentHomeCommand(dept, user)

	@RequestMapping
	def adminDepartment(cmd: AdminDepartmentHomeCommand, @PathVariable("department") dept: Department): Mav = {
		val (modules, routes) = cmd.apply()

		Mav("admin/department",
			"department" -> dept,
			"modules" -> modules,
			"departmentRoutes" -> routes // Stupid workaround for "routes" being used already in Freemarker
		)
	}
}

@Controller
@RequestMapping(Array("/admin/department"))
class AdminDepartmentHomeRedirectController extends AdminController {
	@RequestMapping
	def homeScreen(user: CurrentUser) = Redirect(Routes.admin.home)
}
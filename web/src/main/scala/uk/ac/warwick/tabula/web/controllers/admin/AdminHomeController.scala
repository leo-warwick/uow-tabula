package uk.ac.warwick.tabula.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.{Department, Module, Route}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController

import scala.collection.JavaConverters._

trait AdminDepartmentsModulesAndRoutes {

	self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent =>

	case class Result(departments: Set[Department], modules: Set[Module], routes: Set[Route])

	def ownedDepartmentsModulesAndRoutes(user: CurrentUser): Result = {
		val ownedDepartments =
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.Administer) ++
				moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Route.Administer)
		val ownedModules = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer)
		val ownedRoutes = courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer)
		Result(ownedDepartments, ownedModules, ownedRoutes)
	}
}

@Controller
class AdminHomeController extends AdminController with DepartmentScopedController with AdminDepartmentsModulesAndRoutes
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringCourseAndRouteServiceComponent with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeDepartment")
	override def activeDepartment(department: Department): Option[Department] = retrieveActiveDepartment(None)

	override val departmentPermission: Permission = null

	@ModelAttribute("departmentsWithPermission")
	override def departmentsWithPermission: Seq[Department] = {
		def withSubDepartments(d: Department) = Seq(d) ++ d.children.asScala.toSeq.sortBy(_.fullName)

		val result = ownedDepartmentsModulesAndRoutes(user)
		(result.departments ++ result.modules.map(_.adminDepartment) ++ result.routes.map(_.adminDepartment))
			.toSeq.sortBy(_.fullName).flatMap(withSubDepartments).distinct
	}
	
	@RequestMapping(Array("/admin"))
	def home(@ModelAttribute("activeDepartment") activeDepartment: Option[Department]): Mav = {
		if (activeDepartment.isDefined) {
			Redirect(Routes.department(activeDepartment.get))
		} else {
			val result = ownedDepartmentsModulesAndRoutes(user)

			Mav("admin/home/view",
				"ownedDepartments" -> result.departments,
				"ownedModuleDepartments" -> result.modules.map { _.adminDepartment },
				"ownedRouteDepartments" -> result.routes.map { _.adminDepartment })
		}
	}

}
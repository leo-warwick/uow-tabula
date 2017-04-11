package uk.ac.warwick.tabula.web.controllers.admin.routes

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.{Routes, Mav}
import javax.validation.Valid
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.admin.routes.{SortRoutesCommandState, SortRoutesCommand}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects, SelfValidating}

/**
 * The interface for sorting department routes into
 * child departments.
 */
@Controller
@RequestMapping(value=Array("/admin/department/{department}/sort-routes"))
class SortRoutesController extends AdminController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	type SortRoutesCommand = Appliable[Unit] with GroupsObjects[Route, Department] with SortRoutesCommandState
	validatesSelf[SelfValidating]

	@ModelAttribute("sortRoutesCommand")
	def command(@PathVariable department: Department): SortRoutesCommand = SortRoutesCommand(department)

	override val departmentPermission: Permission = Permissions.Department.ArrangeRoutesAndModules

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand):Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			form(cmd).addObjects("saved" -> true)
		}
	}

	private def form(@ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand): Mav = {
		if (!cmd.department.hasChildren && cmd.department.hasParent) {
			// Sorting is done from the POV of the parent department.
			Redirect(Routes.admin.department.sortRoutes(cmd.department.parent))
		} else {
			Mav("admin/routes/arrange/form").crumbs(
				Breadcrumbs.Department(cmd.department)
			)
		}
	}

}

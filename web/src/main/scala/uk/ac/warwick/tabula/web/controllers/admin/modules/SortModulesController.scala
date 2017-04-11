package uk.ac.warwick.tabula.web.controllers.admin.modules

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.permissions.{Permissions, Permission}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.{Routes, Mav}
import javax.validation.Valid
import uk.ac.warwick.tabula.web.controllers.admin.{AdminBreadcrumbs, AdminController}
import uk.ac.warwick.tabula.commands.admin.modules.{SortModulesCommandState, SortModulesCommand}
import uk.ac.warwick.tabula.commands.{GroupsObjects, SelfValidating, Appliable}

/**
 * The interface for sorting department modules into
 * child departments (or module groups, whatever you want to call them).
 */
@Controller
@RequestMapping(value=Array("/admin/department/{department}/sort-modules"))
class SortModulesController extends AdminController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	type SortModulesCommand = Appliable[Unit] with GroupsObjects[Module, Department] with SortModulesCommandState
	validatesSelf[SelfValidating]

	@ModelAttribute("sortModulesCommand")
	def command(@PathVariable department: Department): SortModulesCommand = SortModulesCommand(department)

	override val departmentPermission: Permission = Permissions.Department.ArrangeRoutesAndModules

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("sortModulesCommand") cmd: SortModulesCommand):Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("sortModulesCommand") cmd: SortModulesCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			form(cmd).addObjects("saved" -> true)
		}
	}

	private def form(@ModelAttribute("sortModulesCommand") cmd: SortModulesCommand): Mav = {
		if (!cmd.department.hasChildren && cmd.department.hasParent) {
			// Sorting is done from the POV of the parent department.
			Redirect(Routes.admin.department.sortModules(cmd.department.parent))
		} else {
			Mav("admin/modules/arrange/form").crumbs(
				Breadcrumbs.Department(cmd.department)
			)
		}
	}

}

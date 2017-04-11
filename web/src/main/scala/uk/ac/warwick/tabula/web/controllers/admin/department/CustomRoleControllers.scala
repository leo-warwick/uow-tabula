package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.admin.department._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import javax.validation.Valid

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.{RoleDefinition, SelectorBuiltInRoleDefinition}
import uk.ac.warwick.tabula.services.RelationshipService

trait CustomRoleControllerMethods extends AdminController {
	validatesSelf[SelfValidating]

	var permissionsService: PermissionsService = Wire[PermissionsService]
	var relationshipService: RelationshipService = Wire[RelationshipService]

	@ModelAttribute("allRoleDefinitions")
	def grantableRoleDefinitions(@PathVariable department: Department, user: CurrentUser): Seq[RoleDefinition] = transactional(readOnly = true) {
		val builtInRoleDefinitions = ReflectionHelper.allBuiltInRoleDefinitions

		def parentDepartments[B <: PermissionsTarget](permissionsTarget: B): Seq[Department] = permissionsTarget match {
			case department: Department => Seq(department)
			case _ => permissionsTarget.permissionsParents.flatMap(parentDepartments)
		}

		val allDepartments = parentDepartments(department)

		val relationshipTypes =
			if (allDepartments.isEmpty) relationshipService.allStudentRelationshipTypes.filter { _.defaultDisplay }
			else allDepartments.flatMap { _.displayedStudentRelationshipTypes }.distinct

		val selectorBuiltInRoleDefinitions =
			ReflectionHelper.allSelectorBuiltInRoleDefinitionNames.flatMap { name =>
				SelectorBuiltInRoleDefinition.of(name, PermissionsSelector.Any[StudentRelationshipType]) +:
					relationshipTypes.map { relationshipType =>
						SelectorBuiltInRoleDefinition.of(name, relationshipType)
					}
			}

		val customRoleDefinitions = allDepartments.flatMap { department => permissionsService.getCustomRoleDefinitionsFor(department) }

		(builtInRoleDefinitions ++ customRoleDefinitions ++ selectorBuiltInRoleDefinitions).filter { _.isAssignable }
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles", "/admin/department/{department}/customroles/list"))
class ListCustomRolesController extends CustomRoleControllerMethods {
	import ListCustomRolesCommand._

	type ListCustomRolesCommand = Appliable[Seq[CustomRoleInfo]] with ListCustomRolesCommandState

	@ModelAttribute("command") def command(@PathVariable department: Department) = ListCustomRolesCommand(department)

	@RequestMapping
	def list(@ModelAttribute("command") command: ListCustomRolesCommand): Mav = {
		Mav("admin/department/customroles/list",
			"customRoles" -> command.apply()
		).crumbs(
			Breadcrumbs.Department(command.department)
		)
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/add"))
class AddCustomRoleDefinitionController extends CustomRoleControllerMethods {

	type AddCustomRoleDefinitionCommand = Appliable[CustomRoleDefinition] with AddCustomRoleDefinitionCommandState

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = AddCustomRoleDefinitionCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@PathVariable department: Department): Mav =
		Mav("admin/department/customroles/add").crumbs(
			Breadcrumbs.Department(department)
		)

	@RequestMapping(method=Array(POST))
	def save(@Valid @ModelAttribute("command") command: AddCustomRoleDefinitionCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(command.department)
		} else {
			command.apply()
			Redirect(Routes.admin.department.customRoles(command.department))
		}
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/{customRoleDefinition}/edit"))
class EditCustomRoleDefinitionController extends CustomRoleControllerMethods {

	type EditCustomRoleDefinitionCommand = Appliable[CustomRoleDefinition] with EditCustomRoleDefinitionCommandState

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable customRoleDefinition: CustomRoleDefinition) =
		EditCustomRoleDefinitionCommand(department, customRoleDefinition)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@PathVariable department: Department): Mav =
		Mav("admin/department/customroles/edit").crumbs(
			Breadcrumbs.Department(department)
		)

	@RequestMapping(method=Array(POST))
	def save(@Valid @ModelAttribute("command") command: EditCustomRoleDefinitionCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(command.department)
		} else {
			command.apply()
			Redirect(Routes.admin.department.customRoles(command.customRoleDefinition.department))
		}
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/{customRoleDefinition}/delete"))
class DeleteCustomRoleDefinitionController extends CustomRoleControllerMethods {

	type DeleteCustomRoleDefinitionCommand = Appliable[CustomRoleDefinition] with DeleteCustomRoleDefinitionCommandState

	@ModelAttribute("command") def command(@PathVariable department: Department, @PathVariable customRoleDefinition: CustomRoleDefinition) =
		DeleteCustomRoleDefinitionCommand(department, customRoleDefinition)

	@RequestMapping(method=Array(GET, HEAD))
	def form(): Mav = Mav("admin/department/customroles/delete").noLayoutIf(ajax)

	@RequestMapping(method=Array(POST))
	def save(@Valid @ModelAttribute("command") command: DeleteCustomRoleDefinitionCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form()
		} else {
			command.apply()
			Redirect(Routes.admin.department.customRoles(command.customRoleDefinition.department))
		}
	}
}

trait CustomRoleOverridesControllerMethods extends AdminController {
	validatesSelf[SelfValidating]

	@ModelAttribute("allPermissions") def allPermissions(@PathVariable department: Department): Map[String, Seq[(String, String)]] = {
		def groupFn(p: Permission) = {
			val simpleName = Permissions.shortName(p.getClass)

			val parentName =
				if (simpleName.indexOf('.') == -1) ""
				else simpleName.substring(0, simpleName.lastIndexOf('.'))

			parentName
		}

		val relationshipTypes = department.displayedStudentRelationshipTypes

		ReflectionHelper.allPermissions
			.filter { p => groupFn(p).hasText }
			.sortBy { p => groupFn(p) }
			.flatMap { p =>
			if (p.isInstanceOf[SelectorPermission[_]]) {
				p +: relationshipTypes.map { relationshipType =>
					SelectorPermission.of(p.getName, relationshipType)
				}
			} else {
				Seq(p)
			}
		}
			.groupBy(groupFn)
			.map { case (key, value) => (key, value.map { p =>
			val name = p match {
				case p: SelectorPermission[_] => p.toString()
				case _ => p.getName
			}

			(name, name)
		})}
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/{customRoleDefinition}/overrides", "/admin/department/{department}/customroles/{customRoleDefinition}/overrides/list"))
class ListCustomRoleDefinitionOverridesController extends CustomRoleOverridesControllerMethods {
	import ListCustomRoleOverridesCommand._

	type ListCustomRoleOverridesCommand = Appliable[CustomRoleOverridesInfo] with ListCustomRoleOverridesCommandState

	@ModelAttribute("command") def command(@PathVariable department: Department, @PathVariable customRoleDefinition: CustomRoleDefinition) =
		ListCustomRoleOverridesCommand(department, customRoleDefinition)

	@RequestMapping
	def list(@ModelAttribute("command") command: ListCustomRoleOverridesCommand): Mav = {
		Mav("admin/department/customroles/overrides",
			"roleInfo" -> command.apply()
		).crumbs(
			Breadcrumbs.Department(command.department)
		)
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/{customRoleDefinition}/overrides/add"))
class AddCustomRoleOverrideController extends CustomRoleOverridesControllerMethods {

	type AddCustomRoleOverrideCommand = Appliable[RoleOverride] with AddCustomRoleOverrideCommandState

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable customRoleDefinition: CustomRoleDefinition) =
		AddCustomRoleOverrideCommand(department, customRoleDefinition)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@PathVariable department: Department): Mav =
		Mav("admin/department/customroles/addOverride").crumbs(
			Breadcrumbs.Department(department)
		)

	@RequestMapping(method=Array(POST))
	def save(@Valid @ModelAttribute("command") command: AddCustomRoleOverrideCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(command.department)
		} else {
			command.apply()
			Redirect(Routes.admin.department.customRoles.overrides(command.customRoleDefinition))
		}
	}
}

@Controller @RequestMapping(Array("/admin/department/{department}/customroles/{customRoleDefinition}/overrides/{roleOverride}/delete"))
class DeleteCustomRoleOverrideController extends CustomRoleOverridesControllerMethods {

	type DeleteCustomRoleOverrideCommand = Appliable[RoleOverride] with DeleteCustomRoleOverrideCommandState

	@ModelAttribute("command") def command(@PathVariable department: Department, @PathVariable customRoleDefinition: CustomRoleDefinition, @PathVariable roleOverride: RoleOverride) =
		DeleteCustomRoleOverrideCommand(department, customRoleDefinition, roleOverride)

	@RequestMapping(method=Array(GET, HEAD))
	def form(): Mav = Mav("admin/department/customroles/deleteOverride").noLayoutIf(ajax)

	@RequestMapping(method=Array(POST))
	def save(@Valid @ModelAttribute("command") command: DeleteCustomRoleOverrideCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form()
		} else {
			command.apply()
			Redirect(Routes.admin.department.customRoles.overrides(command.customRoleDefinition))
		}
	}
}
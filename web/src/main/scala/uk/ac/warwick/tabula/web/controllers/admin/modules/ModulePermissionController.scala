package uk.ac.warwick.tabula.web.controllers.admin.modules

import scala.collection.JavaConverters._
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.permissions.{RevokeRoleCommandState, GrantRoleCommandState, GrantRoleCommand, RevokeRoleCommand}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole


trait ModulePermissionControllerMethods extends AdminController {

	type GrantRoleCommand = Appliable[GrantedRole[Module]] with GrantRoleCommandState[Module]
	type RevokeRoleCommand = Appliable[GrantedRole[Module]] with RevokeRoleCommandState[Module]

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable module: Module): GrantRoleCommand = GrantRoleCommand(module)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable module: Module): RevokeRoleCommand = RevokeRoleCommand(module)

	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	def form(module: Module): Mav = {
		Mav("admin/modules/permissions", "module" -> module)
			.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	def form(module: Module, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.asJava).asScala
		Mav("admin/modules/permissions",
				"module" -> module,
				"users" -> users,
				"role" -> role,
				"action" -> action)
			.crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModulePermissionController extends AdminController with ModulePermissionControllerMethods {

	@RequestMapping
	def permissionsForm(@PathVariable module: Module, @RequestParam(defaultValue="") usercodes: Array[String],
		@RequestParam(value="role", required=false) role: RoleDefinition, @RequestParam(value="action", required=false) action: String): Mav =
		form(module, usercodes, Some(role), action)
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleAddPermissionController extends AdminController with ModulePermissionControllerMethods {

	validatesSelf[SelfValidating]

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand, errors: Errors) : Mav = {
		val module = command.scope
		if (errors.hasErrors()) {
			form(module)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(module, userCodes, role, "add")
		}
	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleRemovePermissionController extends AdminController with ModulePermissionControllerMethods {

	validatesSelf[SelfValidating]

	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand,
	                     errors: Errors): Mav = {
		val module = command.scope
		if (errors.hasErrors()) {
			form(module)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(module, userCodes, role, "remove")
		}
	}
}

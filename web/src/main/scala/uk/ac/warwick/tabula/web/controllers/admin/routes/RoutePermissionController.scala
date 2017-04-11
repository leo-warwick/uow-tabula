package uk.ac.warwick.tabula.web.controllers.admin.routes

import scala.collection.JavaConverters._
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.permissions.{RevokeRoleCommandState, RevokeRoleCommand, GrantRoleCommandState, GrantRoleCommand}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole


trait RoutePermissionControllerMethods extends AdminController {

	type GrantRoleCommand = Appliable[GrantedRole[Route]] with GrantRoleCommandState[Route]
	type RevokeRoleCommand = Appliable[GrantedRole[Route]] with RevokeRoleCommandState[Route]

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable route: Route): GrantRoleCommand = GrantRoleCommand(route)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable route: Route): RevokeRoleCommand = RevokeRoleCommand(route)

	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	def form(route: Route): Mav = {
		Mav("admin/routes/permissions", "route" -> route)
			.crumbs(Breadcrumbs.Department(route.adminDepartment), Breadcrumbs.Route(route))
	}

	def form(route: Route, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.asJava).asScala
		Mav("admin/routes/permissions",
				"route" -> route,
				"users" -> users,
				"role" -> role,
				"action" -> action)
			.crumbs(Breadcrumbs.Department(route.adminDepartment), Breadcrumbs.Route(route))
	}
}

@Controller @RequestMapping(value = Array("/admin/route/{route}/permissions"))
class RoutePermissionController extends AdminController with RoutePermissionControllerMethods {

	@RequestMapping
	def permissionsForm(@PathVariable route: Route, @RequestParam(defaultValue="") usercodes: Array[String],
		@RequestParam(value="role", required=false) role: RoleDefinition, @RequestParam(value="action", required=false) action: String): Mav =
		form(route, usercodes, Some(role), action)
}

@Controller @RequestMapping(value = Array("/admin/route/{route}/permissions"))
class RouteAddPermissionController extends AdminController with RoutePermissionControllerMethods {

	validatesSelf[SelfValidating]

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand, errors: Errors) : Mav = {
		val route = command.scope
		if (errors.hasErrors()) {
			form(route)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(route, userCodes, role, "add")
		}
	}
}

@Controller @RequestMapping(value = Array("/admin/route/{route}/permissions"))
class RouteRemovePermissionController extends AdminController with RoutePermissionControllerMethods {

	validatesSelf[SelfValidating]

	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand,
	                     errors: Errors): Mav = {
		val route = command.scope
		if (errors.hasErrors()) {
			form(route)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(route, userCodes, role, "remove")
		}
	}
}

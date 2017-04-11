package uk.ac.warwick.tabula.web.controllers.admin.permissions

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.admin.permissions.BuildPermissionsTreeCommand
import uk.ac.warwick.tabula.commands.admin.permissions.BuildPermissionsTreeCommand.PermissionsTree
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Route, Module, Department}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.web.Mav

import scala.reflect.ClassTag

abstract class PermissionsTreeControllerMethods[A <: PermissionsTarget : ClassTag] extends AdminController {

	type PermissionsTreeCommand = Appliable[PermissionsTree[A]]
	@ModelAttribute("command") def command(@PathVariable target: A): PermissionsTreeCommand = BuildPermissionsTreeCommand(target)

	@RequestMapping
	def tree(@ModelAttribute("command") command: PermissionsTreeCommand, @PathVariable target: A): Mav = {
		Mav("admin/permissions/tree",
			"tree" -> command.apply()
		).crumbs(Breadcrumbs.Permissions(target))
	}

}

@Controller @RequestMapping(value = Array("/admin/permissions/department/{target}/tree"))
class DepartmentPermissionsTreeController extends PermissionsTreeControllerMethods[Department]

@Controller @RequestMapping(value = Array("/admin/permissions/module/{target}/tree"))
class ModulePermissionsTreeController extends PermissionsTreeControllerMethods[Module]

@Controller @RequestMapping(value = Array("/admin/permissions/route/{target}/tree"))
class RoutePermissionsTreeController extends PermissionsTreeControllerMethods[Route]
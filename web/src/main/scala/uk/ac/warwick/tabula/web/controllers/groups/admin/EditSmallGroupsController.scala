package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.{EditSmallGroupsCommand, PopulateEditSmallGroupsCommand}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

abstract class AbstractEditSmallGroupsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupsCommand = Appliable[Seq[SmallGroup]] with PopulateEditSmallGroupsCommand

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupsCommand =
		EditSmallGroupsCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet, model: (String, _)*): Mav = {
		Mav(renderPath, model:_*).crumbs(Breadcrumbs.Department(set.module.adminDepartment, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupsCommand
	): Mav = render(set)

	protected def submit(cmd: EditSmallGroupsCommand, errors: Errors, set: SmallGroupSet, route: String): Mav = {
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = {
		if (!errors.hasErrors) {
			cmd.apply()
			cmd.populate()
		}

		render(set, "saved" -> true)
	}

	@RequestMapping(method = Array(POST))
	def saveAndExit(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin(set.module.adminDepartment, set.academicYear))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/groups"))
@Controller
class CreateSmallGroupSetAddGroupsController extends AbstractEditSmallGroupsController {

	override val renderPath = "groups/admin/groups/newgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties, "action!=update"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=update"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents, "action!=update"))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=update"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/groups"))
@Controller
class EditSmallGroupSetAddGroupsController extends AbstractEditSmallGroupsController {

	override val renderPath = "groups/admin/groups/editgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties, "action!=update"))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=update"))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents, "action!=update"))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=update"))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAllocate(set))

}

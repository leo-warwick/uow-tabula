package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.{EditSmallGroupSetMembershipCommand, ModifiesSmallGroupSetMembership}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

abstract class AbstractEditSmallGroupSetMembershipController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupSetMembershipCommand = Appliable[SmallGroupSet] with ModifiesSmallGroupSetMembership

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupSetMembershipCommand =
		EditSmallGroupSetMembershipCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet, cmd: EditSmallGroupSetMembershipCommand, model: Map[String, _] = Map()): Mav = {
		Mav(renderPath, model ++ Map(
			"department" -> cmd.module.adminDepartment,
			"module" -> cmd.module,
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups))
			.crumbs(Breadcrumbs.Department(set.module.adminDepartment, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand
	): Mav = {
		cmd.copyGroupsFrom(set)
		cmd.afterBind()

		render(set, cmd)
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def update(@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav = {
		cmd.afterBind()

		if (!errors.hasErrors) {
			cmd.apply()
		}

		cmd.copyGroupsFrom(set)
		cmd.afterBind()

		render(set, cmd, Map("saved" -> true))
	}

	protected def submit(cmd: EditSmallGroupSetMembershipCommand, errors: Errors, set: SmallGroupSet, route: String): Mav = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin(set.module.adminDepartment, set.academicYear))

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/students"))
@Controller
class CreateSmallGroupSetAddStudentsController extends AbstractEditSmallGroupSetMembershipController {

	override val renderPath = "groups/admin/groups/newstudents"

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.createAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.createAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/students"))
@Controller
class EditSmallGroupSetAddStudentsController extends AbstractEditSmallGroupSetMembershipController {

	override val renderPath = "groups/admin/groups/editstudents"

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.editAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.editAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.editAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAddEvents(set))

	@RequestMapping(method = Array(POST), params = Array("action!=update", ManageSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.editAllocate(set))

}
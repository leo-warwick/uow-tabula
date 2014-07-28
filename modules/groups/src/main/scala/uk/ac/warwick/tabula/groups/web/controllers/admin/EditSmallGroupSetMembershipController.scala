package uk.ac.warwick.tabula.groups.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.commands.{UpstreamGroup, UpstreamGroupPropertyEditor, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.{ModifiesSmallGroupSetMembership, EditSmallGroupSetMembershipCommand}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractEditSmallGroupSetMembershipController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditSmallGroupSetMembershipCommand = Appliable[SmallGroupSet] with ModifiesSmallGroupSetMembership

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable("module") module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupSetMembershipCommand =
		EditSmallGroupSetMembershipCommand(module, set)

	protected def renderPath: String

	protected def render(set: SmallGroupSet, cmd: EditSmallGroupSetMembershipCommand) = {
		Mav(renderPath,
			"department" -> cmd.module.department,
			"module" -> cmd.module,
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups)
			.crumbs(Breadcrumbs.Department(set.module.department), Breadcrumbs.Module(set.module))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: SmallGroupSet,
		@ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand
	) = {
		cmd.afterBind()
		cmd.copyGroupsFrom(set)

		render(set, cmd)
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def update(@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet) = {
		cmd.afterBind()

		if (!errors.hasErrors) {
			cmd.apply()
		}

		form(set, cmd)
	}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(set.module))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/new/{smallGroupSet}/students"))
@Controller
class CreateSmallGroupSetAddStudentsController extends AbstractEditSmallGroupSetMembershipController {

	override val renderPath = "admin/groups/newstudents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAddEvents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.createAllocate(set))
		}
	}

}

@RequestMapping(Array("/admin/module/{module}/groups/edit/{smallGroupSet}/students"))
@Controller
class EditSmallGroupSetAddStudentsController extends AbstractEditSmallGroupSetMembershipController {

	override val renderPath = "admin/groups/editstudents"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddGroups(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents))
	def saveAndAddEvents(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAddEvents(set))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditSmallGroupSetMembershipCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: SmallGroupSet
	) = {
		cmd.afterBind()

		if (errors.hasErrors) {
			render(set, cmd)
		} else {
			cmd.apply()
			RedirectForce(Routes.admin.editAllocate(set))
		}
	}

}
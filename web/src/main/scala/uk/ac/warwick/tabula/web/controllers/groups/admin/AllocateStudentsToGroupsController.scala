package uk.ac.warwick.tabula.web.controllers.groups.admin

import uk.ac.warwick.tabula.commands.{GroupsObjects, Appliable, SelfValidating}
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.commands.groups.admin.AllocateStudentsToGroupsCommand
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.userlookup.User

/**
 * Allocates students that are in the allocation list for groups to individual groups.
 */
abstract class AbstractAllocateStudentsToGroupsController extends GroupsController {

	validatesSelf[SelfValidating]
	type AllocateStudentsToGroupsCommand = Appliable[SmallGroupSet] with GroupsObjects[User, SmallGroup]

	@ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable smallGroupSet: SmallGroupSet): AllocateStudentsToGroupsCommand =
		AllocateStudentsToGroupsCommand(module, smallGroupSet, user)

	@RequestMapping
	def showForm(@ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav = {
		cmd.populate()
		cmd.sort()
		render(set)
	}

	protected val renderPath: String

	protected def render(set: SmallGroupSet): Mav = {
		Mav(renderPath).crumbs(Breadcrumbs.Department(set.module.adminDepartment, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
	}

	protected def submit(cmd: AllocateStudentsToGroupsCommand, errors: Errors, set: SmallGroupSet, route: String, objects: (String, _)*): Mav = {
		cmd.sort()
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route, objects: _*)
		}
	}

	@RequestMapping(method=Array(POST))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.module(set.module, set.academicYear), "allocated" -> set.id)

}

@Controller
@RequestMapping(value=Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/allocate"))
class CreateSmallGroupSetAllocateController extends AbstractAllocateStudentsToGroupsController {
	override protected val renderPath = "groups/admin/groups/newallocate"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties))
	def saveAndEditProperties(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndEditGroups(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.createAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents))
	def saveAndEditStudents(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents))
	def saveAndEditEvents(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.createAddEvents(set))
}

@Controller
@RequestMapping(value=Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/allocate"))
class EditSmallGroupSetAllocateController extends AbstractAllocateStudentsToGroupsController {
	override protected val renderPath = "groups/admin/groups/editallocate"

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties))
	def saveAndEditProperties(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups))
	def saveAndEditGroups(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.editAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents))
	def saveAndEditStudents(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents))
	def saveAndEditEvents(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsCommand, errors: Errors, @PathVariable("smallGroupSet") set: SmallGroupSet): Mav =
		submit(cmd, errors, set, Routes.admin.editAddEvents(set))
}
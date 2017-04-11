package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.groups.admin.reusable.EditDepartmentSmallGroupsCommand
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

abstract class AbstractEditDepartmentSmallGroupsController extends GroupsController {

	validatesSelf[SelfValidating]

	type EditDepartmentSmallGroupsCommand = Appliable[Seq[DepartmentSmallGroup]] with PopulateOnForm

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	@ModelAttribute("command") def command(@PathVariable department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupsCommand =
		EditDepartmentSmallGroupsCommand(department, set)

	protected def render(set: DepartmentSmallGroupSet): Mav =
		Mav(renderPath).crumbs(Breadcrumbs.Department(set.department, set.academicYear), Breadcrumbs.Reusable(set.department, set.academicYear))

	protected val renderPath: String

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet,
		@ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand
	): Mav = {
		cmd.populate()
		render(set)
	}

	protected def submit(cmd: EditDepartmentSmallGroupsCommand, errors: Errors, set: DepartmentSmallGroupSet, route: String): Mav =
		if (errors.hasErrors) {
			render(set)
		} else {
			cmd.apply()
			RedirectForce(route)
		}

	@RequestMapping(method = Array(POST))
	def save(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable(set.department, set.academicYear))

}

@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/new/{smallGroupSet}/groups"))
@Controller
class CreateDepartmentSmallGroupSetAddGroupsController extends AbstractEditDepartmentSmallGroupsController {

	override protected val renderPath = "groups/admin/groups/reusable/newgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.createAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/edit/{smallGroupSet}/groups"))
@Controller
class EditDepartmentSmallGroupSetAddGroupsController extends AbstractEditDepartmentSmallGroupsController {

	override protected val renderPath = "groups/admin/groups/reusable/editgroups"

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents))
	def saveAndAddStudents(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.editAddStudents(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAddAllocate(
		@Valid @ModelAttribute("command") cmd: EditDepartmentSmallGroupsCommand,
		errors: Errors,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, set, Routes.admin.reusable.editAllocate(set))

}

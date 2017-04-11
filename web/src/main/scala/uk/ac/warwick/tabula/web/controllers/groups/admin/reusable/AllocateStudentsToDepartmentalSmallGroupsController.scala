package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet}
import uk.ac.warwick.tabula.commands.groups.admin.reusable.AllocateStudentsToDepartmentalSmallGroupsCommand
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.userlookup.User

/**
 * Allocates students that are in the allocation list for groups to individual groups.
 */
abstract class AllocateStudentsToDepartmentalSmallGroupsController extends GroupsController {

	validatesSelf[SelfValidating]
	type AllocateStudentsToDepartmentalSmallGroupsCommand = Appliable[DepartmentSmallGroupSet] with GroupsObjects[User, DepartmentSmallGroup]

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable smallGroupSet: DepartmentSmallGroupSet): AllocateStudentsToDepartmentalSmallGroupsCommand =
		AllocateStudentsToDepartmentalSmallGroupsCommand(department, smallGroupSet, user)

	@RequestMapping
	def showForm(
		@ModelAttribute("command") cmd: AllocateStudentsToDepartmentalSmallGroupsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd, department, academicYear)
	}

	protected val renderPath: String

	protected def form(cmd: AllocateStudentsToDepartmentalSmallGroupsCommand, department: Department, academicYear: AcademicYear): Mav =
		Mav(renderPath).crumbs(Breadcrumbs.Department(department, academicYear), Breadcrumbs.Reusable(department, academicYear))

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: AllocateStudentsToDepartmentalSmallGroupsCommand,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		cmd.sort()
		if (errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.admin.reusable(department, academicYear))
		}
	}

}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/new/{smallGroupSet}/allocate"))
class CreateDepartmentSmallGroupSetAllocateController extends AllocateStudentsToDepartmentalSmallGroupsController {
	override protected val renderPath = "groups/admin/groups/reusable/allocateoncreate"
}

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/edit/{smallGroupSet}/allocate"))
class EditDepartmentSmallGroupSetAllocateController extends AllocateStudentsToDepartmentalSmallGroupsController {
	override protected val renderPath = "groups/admin/groups/reusable/allocateonedit"
}
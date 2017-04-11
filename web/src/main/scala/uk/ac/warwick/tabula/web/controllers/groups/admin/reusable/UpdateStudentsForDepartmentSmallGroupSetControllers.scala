package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.reusable._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._

abstract class UpdateStudentsForDepartmentSmallGroupSetController extends GroupsController {

	validatesSelf[SelfValidating]

	type UpdateStudentsForDepartmentSmallGroupSetCommand = Appliable[DepartmentSmallGroupSet]
	type FindStudentsForDepartmentSmallGroupSetCommand = Appliable[FindStudentsForDepartmentSmallGroupSetCommandResult] with PopulateOnForm with FindStudentsForDepartmentSmallGroupSetCommandState with UpdatesFindStudentsForDepartmentSmallGroupSetCommand
	type EditDepartmentSmallGroupSetMembershipCommand = Appliable[EditDepartmentSmallGroupSetMembershipCommandResult] with PopulateOnForm with AddsUsersToEditDepartmentSmallGroupSetMembershipCommand with RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand with ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand

	@ModelAttribute("ManageDepartmentSmallGroupsMappingParameters") def params = ManageDepartmentSmallGroupsMappingParameters

	@ModelAttribute("persistenceCommand") def persistenceCommand(@PathVariable department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): UpdateStudentsForDepartmentSmallGroupSetCommand =
		UpdateStudentsForDepartmentSmallGroupSetCommand(mandatory(department), mandatory(set))

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): FindStudentsForDepartmentSmallGroupSetCommand =
		FindStudentsForDepartmentSmallGroupSetCommand(mandatory(department), mandatory(set))

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): EditDepartmentSmallGroupSetMembershipCommand =
		EditDepartmentSmallGroupSetMembershipCommand(mandatory(department), mandatory(set))

	protected val renderPath: String

	private def summaryString(
		findStudentsCommandResult: FindStudentsForDepartmentSmallGroupSetCommandResult,
		editMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult
	): String = {
		val sitsCount = (findStudentsCommandResult.staticStudentIds.asScala
			diff editMembershipCommandResult.excludedStudentIds.asScala
			diff editMembershipCommandResult.includedStudentIds.asScala).size

		val removedCount = editMembershipCommandResult.excludedStudentIds.asScala.count(findStudentsCommandResult.staticStudentIds.asScala.contains)
		val addedCount = editMembershipCommandResult.includedStudentIds.asScala.size

		if (sitsCount == 0)
			""
		else
			s"${sitsCount + addedCount} students on this scheme <span class='very-subtle'>($sitsCount from SITS${removedCount match {
				case 0 => ""
				case count => s" after $count removed manually"
			}}${addedCount match {
				case 0 => ""
				case count => s", plus $count added manually"
			}})</span>"
	}

	protected def render(
		set: DepartmentSmallGroupSet,
		findStudentsCommandResult: FindStudentsForDepartmentSmallGroupSetCommandResult,
		editMembershipCommandResult: EditDepartmentSmallGroupSetMembershipCommandResult,
		addUsersResult: AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult = AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(Seq()),
		expandFind: Boolean = false,
		expandManual: Boolean = false
	): Mav = {
		Mav(renderPath,
			"totalResults" -> 0,
			"findCommandResult" -> findStudentsCommandResult,
			"editMembershipCommandResult" -> editMembershipCommandResult,
			"addUsersResult" -> addUsersResult,
			"summaryString" -> summaryString(findStudentsCommandResult, editMembershipCommandResult),
			"expandFind" -> expandFind,
			"expandManual" -> expandManual,
			"SITSInFlux" -> set.academicYear.isSITSInFlux(DateTime.now),
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Department(set.department, set.academicYear),
			Breadcrumbs.Reusable(set.department, set.academicYear)
		)
	}

	@RequestMapping
	def form(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		findCommand.populate()
		editMembershipCommand.populate()
		val findStudentsCommandResult =
			if (findCommand.filterQueryString.length > 0)
				findCommand.apply()
			else
				FindStudentsForDepartmentSmallGroupSetCommandResult(JArrayList(), Seq())
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.findStudents))
	def findStudents(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		val findStudentsCommandResult = findCommand.apply()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandFind = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyAddForm))
	def manuallyAddForm(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		Mav("groups/admin/groups/reusable/manuallyaddstudents",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Department(set.department, set.academicYear),
			Breadcrumbs.Reusable(set.department, set.academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyAddSubmit))
	def manuallyAddSubmit(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		val addUsersResult = editMembershipCommand.addUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, addUsersResult = addUsersResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.manuallyExclude))
	def manuallyExclude(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		editMembershipCommand.removeUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.resetMembership))
	def resetMembership(
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = {
		editMembershipCommand.resetMembership()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsCommandResult = findCommand.apply()
		render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
	}

	protected def submit(
		cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		set: DepartmentSmallGroupSet,
		route: String
	): Mav = {
		if (errors.hasErrors) {
			val findStudentsCommandResult =
				if (findCommand.filterQueryString.length > 0)
					findCommand.apply()
				else
					FindStudentsForDepartmentSmallGroupSetCommandResult(JArrayList(), Seq())
			val editMembershipCommandResult = editMembershipCommand.apply()
			render(set, findStudentsCommandResult, editMembershipCommandResult)
		} else {
			cmd.apply()
			RedirectForce(route)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("persist"))
	def save(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable(set.department, set.academicYear))

}

@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/new/{smallGroupSet}/students"))
@Controller
class CreateDepartmentSmallGroupSetAddStudentsController extends UpdateStudentsForDepartmentSmallGroupSetController {

	override protected val renderPath = "groups/admin/groups/reusable/addstudentsoncreate"

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.create(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.createAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.createAndAllocate))
	def saveAndAllocate(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/edit/{smallGroupSet}/students"))
@Controller
class EditDepartmentSmallGroupSetAddStudentsController extends UpdateStudentsForDepartmentSmallGroupSetController {

	override protected val renderPath = "groups/admin/groups/reusable/editstudents"

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndEditProperties))
	def saveAndEditProperties(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.edit(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups))
	def saveAndAddGroups(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.editAddGroups(set))

	@RequestMapping(method = Array(POST), params = Array(ManageDepartmentSmallGroupsMappingParameters.editAndAllocate))
	def saveAndAllocate(
		@Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForDepartmentSmallGroupSetCommand,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: FindStudentsForDepartmentSmallGroupSetCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: EditDepartmentSmallGroupSetMembershipCommand,
		@PathVariable("smallGroupSet") set: DepartmentSmallGroupSet
	): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.reusable.editAllocate(set))

}
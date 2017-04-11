package uk.ac.warwick.tabula.web.controllers.attendance.manage

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

import scala.collection.JavaConverters._

abstract class AbstractManageSchemeStudentsController extends AttendanceController with AutowiringTermServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("persistanceCommand")
	def persistanceCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		AddStudentsToSchemeCommand(mandatory(scheme), user)

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		FindStudentsForSchemeCommand(mandatory(scheme), user)

	@ModelAttribute("editMembershipCommand")
	def editMembershipCommand(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(mandatory(scheme), user)

	@ModelAttribute("ManageSchemeMappingParameters")
	def manageSchemeMappingParameters = ManageSchemeMappingParameters

	protected val renderPath: String

	private def summaryString(
		findStudentsForSchemeCommandResult: FindStudentsForSchemeCommandResult,
		editMembershipCommandResult: EditSchemeMembershipCommandResult
	): String = {

		val sitsCount = (findStudentsForSchemeCommandResult.staticStudentIds.asScala
			diff editMembershipCommandResult.excludedStudentIds.asScala
			diff editMembershipCommandResult.includedStudentIds.asScala).size

		val removedCount = editMembershipCommandResult.excludedStudentIds.asScala.count(findStudentsForSchemeCommandResult.staticStudentIds.asScala.contains)
		val addedCount = editMembershipCommandResult.includedStudentIds.asScala.size

		if (sitsCount == 0 && addedCount == 0)
			""
		else
			s"${sitsCount + addedCount} students on this scheme <span class='muted'>($sitsCount from SITS${ removedCount match {
				case 0 => ""
				case count => s" after $count removed manually"
			}}${ addedCount match {
				case 0 => ""
				case count => s", plus $count added manually"
			}})</span>"
	}

	protected def render(
		scheme: AttendanceMonitoringScheme,
		findStudentsForSchemeCommandResult: FindStudentsForSchemeCommandResult,
		editMembershipCommandResult: EditSchemeMembershipCommandResult,
		addUsersResult: AddUsersToEditSchemeMembershipCommandResult = AddUsersToEditSchemeMembershipCommandResult(Seq(), Seq()),
		expandFind: Boolean = false,
		expandManual: Boolean = false
	): Mav = {
		Mav(renderPath,
			"totalResults" -> 0,
			"findCommandResult" -> findStudentsForSchemeCommandResult,
			"editMembershipCommandResult" -> editMembershipCommandResult,
			"addUsersResult" -> addUsersResult,
			"summaryString" -> summaryString(findStudentsForSchemeCommandResult, editMembershipCommandResult),
			"expandFind" -> expandFind,
			"expandManual" -> expandManual,
			"SITSInFlux" -> scheme.academicYear.isSITSInFlux(DateTime.now),
			"returnTo" -> getReturnTo(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		).crumbs(
				Breadcrumbs.Manage.HomeForYear(scheme.academicYear),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with PopulateOnForm with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with PopulateOnForm,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		findCommand.populate()
		editMembershipCommand.populate()
		val findStudentsForSchemeCommandResult = findCommand.apply()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
	}

	@RequestMapping(Array("/all"))
	def allStudents(
		@ModelAttribute("persistanceCommand") cmd: AddStudentsToSchemeCommandState
	): Mav = {
		Mav("attendance/manage/_allstudents", "membershipItems" -> cmd.membershipItems).noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.findStudents))
	def findStudents(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult],
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		val findStudentsForSchemeCommandResult = findCommand.apply()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandFind = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.manuallyAddForm))
	def manuallyAddForm(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult],
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		Mav("attendance/manage/manuallyaddstudents",
			"ManageSchemeMappingParameters" -> ManageSchemeMappingParameters,
			"returnTo" -> getReturnTo("")
		).crumbs(
				Breadcrumbs.Manage.HomeForYear(scheme.academicYear),
				Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.manuallyAddSubmit))
	def manuallyAddSubmit(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with AddsUsersToEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		val addUsersResult = editMembershipCommand.addUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, addUsersResult = addUsersResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.manuallyExclude))
	def manuallyExclude(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with RemovesUsersFromEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		editMembershipCommand.removeUsers()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.resetMembership))
	def resetMembership(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with UpdatesFindStudentsForSchemeCommand,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult] with ResetsMembershipInEditSchemeMembershipCommand,
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		editMembershipCommand.resetMembership()
		val editMembershipCommandResult = editMembershipCommand.apply()
		findCommand.update(editMembershipCommandResult)
		val findStudentsForSchemeCommandResult = findCommand.apply()
		render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult, expandManual = true)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.resetFilter))
	def resetFilter(
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		findCommand.deserializeFilter("")
		findCommand.staticStudentIds.clear()
		val editMembershipCommandResult = editMembershipCommand.apply()
		render(scheme, FindStudentsForSchemeCommandResult(findCommand.staticStudentIds, Seq()), editMembershipCommandResult, expandFind = true)
	}

	@RequestMapping(method = Array(POST), params = Array("persist"))
	def save(
		@Valid @ModelAttribute("persistanceCommand") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult],
		@ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
		@PathVariable scheme: AttendanceMonitoringScheme
	): Mav = {
		if (errors.hasErrors) {
			val findStudentsForSchemeCommandResult = findCommand.apply()
			val editMembershipCommandResult = editMembershipCommand.apply()
			render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.departmentForYear(scheme.department, scheme.academicYear))
		}
	}


}

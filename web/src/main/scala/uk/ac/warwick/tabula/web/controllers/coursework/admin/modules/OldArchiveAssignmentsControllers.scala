package uk.ac.warwick.tabula.web.controllers.coursework.admin.modules

import org.springframework.context.annotation.Profile

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.commands.coursework.assignments.{ArchiveAssignmentsCommand, ArchiveAssignmentsDescription, ArchiveAssignmentsPermissions}
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.services.AutowiringAssessmentServiceComponent
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/archive-assignments"))
class OldArchiveModuleAssignmentsController extends OldCourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def archiveAssignmentsCommand(@PathVariable module: Module) = ArchiveAssignmentsCommand(module.adminDepartment, Seq(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable module: Module, cmd: ArchiveAssignmentsCommand): Mav = {
		Mav(s"$urlPrefix/admin/modules/archive_assignments",
			"title" -> module.name,
			"cancel" -> Routes.admin.module(module),
			"map" -> moduleAssignmentMap(cmd.modules)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveAssignmentsCommand, @PathVariable module: Module, errors: Errors, user: CurrentUser): Mav = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/archive-assignments"))
class OldArchiveDepartmentAssignmentsController extends OldCourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def archiveAssignmentsCommand(@PathVariable department: Department): ArchiveAssignmentsCommand with ComposableCommand[Seq[Assignment]] with ArchiveAssignmentsPermissions with ArchiveAssignmentsDescription with AutowiringAssessmentServiceComponent = {
		val modules = department.modules.asScala.filter(_.assignments.asScala.exists(_.isAlive))
		ArchiveAssignmentsCommand(department, modules)
	}



	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable department: Department, cmd: ArchiveAssignmentsCommand): Mav = {
		Mav(s"$urlPrefix/admin/modules/archive_assignments",
			"title" -> department.name,
			"cancel" -> Routes.admin.department(department),
			"map" -> moduleAssignmentMap(cmd.modules),
			"showSubHeadings" -> true
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: ArchiveAssignmentsCommand, @PathVariable department: Department, errors: Errors, user: CurrentUser): Mav = {
		cmd.apply()
		Redirect(Routes.admin.department(department))
	}
}
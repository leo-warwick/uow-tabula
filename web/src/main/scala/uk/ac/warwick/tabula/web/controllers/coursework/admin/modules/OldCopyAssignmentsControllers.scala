package uk.ac.warwick.tabula.web.controllers.coursework.admin.modules

import org.springframework.context.annotation.Profile

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.coursework.assignments.{CopyAssignmentsCommand, CopyAssignmentsDescription, CopyAssignmentsPermissions}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/copy-assignments"))
class OldCopyModuleAssignmentsController extends OldCourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable module: Module) = CopyAssignmentsCommand(module.adminDepartment, Seq(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable module: Module, cmd: CopyAssignmentsCommand): Mav = {

		Mav(s"$urlPrefix/admin/modules/copy_assignments",
			"title" -> module.name,
			"cancel" -> Routes.admin.module(module),
			"map" -> moduleAssignmentMap(cmd.modules)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable module: Module, errors: Errors, user: CurrentUser): Mav = {
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/copy-assignments"))
class OldCopyDepartmentAssignmentsController extends OldCourseworkController with UnarchivedAssignmentsMap {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable department: Department): CopyAssignmentsCommand with ComposableCommand[Seq[Assignment]] with CopyAssignmentsPermissions with CopyAssignmentsDescription with AutowiringAssessmentServiceComponent with AutowiringAssessmentMembershipServiceComponent = {
		val modules = department.modules.asScala.filter(_.assignments.asScala.exists(_.isAlive)).sortBy { _.code }
		CopyAssignmentsCommand(department, modules)
	}

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable department: Department, cmd: CopyAssignmentsCommand): Mav = {

		Mav(s"$urlPrefix/admin/modules/copy_assignments",
			"title" -> department.name,
			"cancel" -> Routes.admin.department(department),
			"map" -> moduleAssignmentMap(cmd.modules),
			"showSubHeadings" -> true
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable department: Department, errors: Errors, user: CurrentUser): Mav = {
		cmd.apply()
		Redirect(Routes.admin.department(department))
	}

}

trait UnarchivedAssignmentsMap {

	def moduleAssignmentMap(modules: Seq[Module]): Map[String, Seq[Assignment]] = (
		for(module <- modules) yield module.code ->  module.assignments.asScala.filter { _.isAlive }
	).toMap.filterNot(_._2.isEmpty)

}

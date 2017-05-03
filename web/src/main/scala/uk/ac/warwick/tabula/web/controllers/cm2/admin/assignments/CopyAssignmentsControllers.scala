package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.CopyAssignmentsCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.AcademicYear

import scala.collection.JavaConverters._


abstract class AbstractCopyAssignmentsController extends CourseworkController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with AcademicYearScopedController {

	val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

	@ModelAttribute("academicYearChoices")
	def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(0, 1).asJava

}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/module/{module}/copy-assignments"))
class CopyModuleAssignmentsController extends AbstractCopyAssignmentsController with AliveAssignmentsMap {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable module: Module) = CopyAssignmentsCommand(mandatory(module).adminDepartment, Seq(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable module: Module, cmd: CopyAssignmentsCommand): Mav = {

		Mav(s"$urlPrefix/admin/modules/copy_assignments",
			"title" -> module.name,
			"cancel" -> Routes.admin.module(module, academicYear),
			"department" -> module.adminDepartment,
			"map" -> moduleAssignmentMap(cmd.modules)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable module: Module): Mav = {
		cmd.apply()
		Redirect(Routes.admin.module(module, academicYear))
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/department/{department}/copy-assignments"))
class CopyDepartmentAssignmentsController extends AbstractCopyAssignmentsController with AliveAssignmentsMap {

	@ModelAttribute
	def copyAssignmentsCommand(@PathVariable department: Department): CopyAssignmentsCommand = {
		val modules = department.modules.asScala.filter(_.assignments.asScala.exists(_.isAlive)).sortBy {
			_.code
		}
		CopyAssignmentsCommand(mandatory(department), modules)
	}

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable department: Department, cmd: CopyAssignmentsCommand): Mav = {

		Mav(s"$urlPrefix/admin/modules/copy_assignments",
			"title" -> department.name,
			"cancel" -> Routes.admin.department(department, academicYear),
			"map" -> moduleAssignmentMap(cmd.modules),
			"showSubHeadings" -> true
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(cmd: CopyAssignmentsCommand, @PathVariable department: Department): Mav = {
		cmd.apply()
		Redirect(Routes.admin.department(department, academicYear))
	}

}

trait AliveAssignmentsMap {
	def moduleAssignmentMap(modules: Seq[Module]): Map[String, Seq[Assignment]] = (
		for (module <- modules) yield module.code -> module.assignments.asScala.filter {
			_.isAlive
		}
		).toMap.filterNot(_._2.isEmpty)
}
package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, PermissionDeniedException}

import scala.collection.JavaConverters._

class ExamsAdminDepartmentHomeCommand(val department: Department, val academicYear: AcademicYear, val user: CurrentUser) extends Command[Seq[Module]]
with ReadOnly with Unaudited with AutowiringSecurityServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringAssessmentServiceComponent {

	val modules: Seq[Module] =
		if (securityService.can(user, Permissions.Module.ManageAssignments, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			PermissionCheck(Permissions.Module.ManageAssignments, department)

			department.modules.asScala.toSeq
		} else {
			val managedModules = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments, department).toList

			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.ManageAssignments, managedModules)

			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.ManageAssignments, department)

			managedModules
		}

	lazy val modulesAndExams: Map[Module, Seq[Exam]] = assessmentService.getExamsByModules(modules, academicYear).withDefaultValue(Seq())

	def applyInternal(): Seq[Module] = {
		benchmarkTask("Sort modules") { modules.sortBy { module => (modulesAndExams(module).isEmpty, module.code) } }
	}

}

case class ExamsDepartmentHomeInformation(modules: Seq[Module], notices: Map[String, Seq[Exam]])

/**
 * Screens for department and module admins.
 */

@Controller
@RequestMapping(Array("/exams/exams/admin", "/exams/exams/admin/department", "/exams/exams/admin/module"))
class ExamsAdminHomeController extends ExamsController {
	@RequestMapping(method=Array(GET, HEAD))
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)
}

@Controller
@RequestMapping(Array("/exams/exams/admin"))
class ExamsAdminHomeDefaultAcademicYearController extends ExamsController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping(value = Array("/department/{department}"), method = Array(GET, HEAD))
	def handleDepartment(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) =
		Redirect(Routes.Exams.admin.department(mandatory(department), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))

	@RequestMapping(value = Array("/module/{module}"), method = Array(GET, HEAD))
	def handleModule(@PathVariable module: Module, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) =
		Redirect(Routes.Exams.admin.module(mandatory(module), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))
}

@Controller
@RequestMapping(value=Array("/exams/exams/admin/department/{department}/{academicYear}"))
class ExamsAdminDepartmentHomeController extends ExamsController
	with DepartmentScopedController with AutowiringModuleAndDepartmentServiceComponent with AutowiringUserSettingsServiceComponent
	with AcademicYearScopedController with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	override def departmentPermission: Permission = Permissions.Module.ManageAssignments

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, user: CurrentUser) =
		new ExamsAdminDepartmentHomeCommand(mandatory(department), academicYear, user)

	@ModelAttribute("academicYears") def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava

	@RequestMapping
	def adminDepartment(cmd: ExamsAdminDepartmentHomeCommand, @PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		val result = cmd.apply()

		Mav("exams/exams/admin/department",
			"department" -> department,
			"modules" -> result.sortWith(_.code.toLowerCase < _.code.toLowerCase),
			"examMap" -> cmd.modulesAndExams
		)
			.crumbs(Breadcrumbs.Exams.Home)
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Exams.admin.department(department, year)):_*)
	}

}

@Controller
@RequestMapping(value=Array("/exams/exams/admin/module/{module}/{academicYear}"))
class ExamsAdminModuleHomeController extends ExamsController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	@Autowired var examService: AssessmentService = _

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable module: Module, user: CurrentUser) =
		new ViewViewableCommand(Permissions.Module.ManageAssignments, module)

	@ModelAttribute("examMap")
	def examMap(@PathVariable module: Module, @PathVariable academicYear: AcademicYear): Map[Module, Seq[Exam]] =
		examService.getExamsByModules(Seq(module), academicYear).withDefaultValue(Seq())

	@RequestMapping
	def adminModule(@ModelAttribute("command") cmd: Appliable[Module], @PathVariable module: Module, @PathVariable academicYear: AcademicYear): Mav = {
		val module = cmd.apply()

		if (ajax) Mav("exams/exams/admin/modules/admin_partial").noLayout()
		else Mav("exams/exams/admin/modules/admin")
			.crumbs(
				Breadcrumbs.Exams.Home,
				Breadcrumbs.Exams.Department(module.adminDepartment, academicYear)
			).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Exams.admin.module(module, year)):_*)
	}
}
package uk.ac.warwick.tabula.web.controllers.exams.grids

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.{ManageCourseYearWeightingsCommand, ManageCourseYearWeightingsCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, FilterStudentsOrRelationships, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/weightings"))
class ManageCourseYearWeightingsController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	validatesSelf[SelfValidating]

	override val departmentPermission: Permission = ManageCourseYearWeightingsCommand.RequiredPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	type CommandType = Appliable[ManageCourseYearWeightingsCommand.Result] with PopulateOnForm with ManageCourseYearWeightingsCommandState

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): CommandType =
		ManageCourseYearWeightingsCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: CommandType): Mav = {
		cmd.populate()
		render(cmd)
	}

	private def render(cmd: CommandType) = {
		Mav("exams/grids/weightings",
			"allYearsOfStudy" -> (1 to FilterStudentsOrRelationships.MaxYearsOfStudy)
		).secondCrumbs(academicYearBreadcrumbs(cmd.startAcademicYear)(year => Routes.Grids.weightings(cmd.department, year)):_*)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: CommandType,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			render(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.Grids.departmentAcademicYear(department, academicYear))
		}
	}

	@RequestMapping(path = Array("/fetch/{otherAcademicYear}"))
	def fetch(
		@ModelAttribute("command") cmd: CommandType,
		@PathVariable department: Department,
		@PathVariable otherAcademicYear: AcademicYear
	): Mav = {
		val weightings = cmd.getWeightings(otherAcademicYear)
		Mav(new JSONView(
			weightings.map { case (course, yearMap) =>
				course.code -> yearMap.map { case (year, weightingOption) =>
					year -> weightingOption.map(_.weightingAsPercentage.toPlainString).getOrElse("")
				}
			}
		))
	}

}

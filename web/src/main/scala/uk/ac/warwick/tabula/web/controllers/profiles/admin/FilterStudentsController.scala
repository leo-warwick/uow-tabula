package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.profiles.{FilterStudentsCommand, FilterStudentsResults}
import uk.ac.warwick.tabula.commands.{Appliable, CurrentSITSAcademicYear}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/students/{academicYear}"))
class FilterStudentsAcademicYearController extends ProfilesController
	with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("filterStudentsCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterStudentsCommand(mandatory(department), mandatory(academicYear))


	@RequestMapping
	def filter(@ModelAttribute("filterStudentsCommand") cmd: Appliable[FilterStudentsResults], errors: Errors, @PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		if (errors.hasErrors) {
			Mav("profiles/profile/filter/filter").noLayout()
		} else {
			val results = cmd.apply()
			val modelMap = Map(
				"students" -> results.students,
				"totalResults" -> results.totalResults,
				"academicYear" -> academicYear
			)
			if (ajax) Mav("profiles/profile/filter/results", modelMap).noLayout()
			else Mav("profiles/profile/filter/filter", modelMap)
				.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.Profile.students(department, year)): _*)
		}
	}
}

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/students"))
class FilterStudentsController extends ProfilesController
	with  AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent
	with CurrentSITSAcademicYear {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

	@RequestMapping
	def filter(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		Redirect(Routes.Profile.students(department, activeAcademicYear.getOrElse(academicYear)))
	}
}
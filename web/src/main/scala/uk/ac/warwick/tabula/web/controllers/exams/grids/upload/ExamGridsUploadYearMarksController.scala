package uk.ac.warwick.tabula.web.controllers.exams.grids.upload

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.exams.grids.{UploadYearMarksCommand, UploadYearMarksCommandState}
import uk.ac.warwick.tabula.data.model.{Department, StudentCourseYearDetails}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/upload"))
class ExamGridsUploadYearMarksController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(department, year)): _*)

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		UploadYearMarksCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav = {
		commonCrumbs(Mav("exams/grids/upload/form"), department, academicYear)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def extractAndPreview(@ModelAttribute("command") cmd: Appliable[Seq[StudentCourseYearDetails]] with UploadYearMarksCommandState,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			val processedItems = cmd.processedYearMarks
			val (invalidItems, validItems) = processedItems.partition(_.errors.nonEmpty)
			val guessedSCJ = validItems.filter(i => i.scjCode != i.yearMarkItem.studentId)
			val defaultAcademicYear = validItems.filter(i => i.yearMarkItem.academicYear.maybeText.isEmpty)
			val roundedMark = validItems.filter(i => i.mark.toString != i.yearMarkItem.mark)
			commonCrumbs(Mav("exams/grids/upload/preview",
				"invalidItems" -> invalidItems,
				"validItems" -> validItems.sortBy(i => (i.academicYear, i.scjCode)),
				"guessedSCJ" -> guessedSCJ,
				"defaultAcademicYear" -> defaultAcademicYear,
				"roundedMark" -> roundedMark
			), department, academicYear)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm"))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[StudentCourseYearDetails]] with UploadYearMarksCommandState,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val scyds = cmd.apply()
		Redirect(Routes.exams.Grids.departmentAcademicYear(department, academicYear), Map("updatedMarks" -> scyds.size))
	}

}

package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.cm2.assignments.ListEnhancedAssignmentsCommand
import uk.ac.warwick.tabula.commands.cm2.assignments.ListEnhancedAssignmentsCommand.AssignmentCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/detail/{assignment}"))
class AssignmentEnhancedDetailsController extends CourseworkController
	with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): AssignmentCommand = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.now())
		ListEnhancedAssignmentsCommand.assignment(assignment, academicYear, user)
	}

	@RequestMapping
	def enhancedDetailsAjax(@ModelAttribute("command") command: AssignmentCommand, @PathVariable assignment: Assignment): Mav =
		Mav("cm2/admin/home/single_enhanced_assignment", "assignmentInfo" -> command.apply(), "academicYear" -> command.academicYear).noLayout()
}

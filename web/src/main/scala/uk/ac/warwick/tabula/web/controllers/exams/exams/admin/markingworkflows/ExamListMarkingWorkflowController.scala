package uk.ac.warwick.tabula.web.controllers.exams.exams.admin.markingworkflows

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{ListMarkingWorkflowCommand, ListMarkingWorkflowCommandResult}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value=Array("/exams/exams/admin/department/{department}/markingworkflows"))
class ExamListMarkingWorkflowController extends ExamsController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("command")
	def command(@PathVariable department: Department) = ListMarkingWorkflowCommand(department, isExam = true)

	@RequestMapping
	def list(
		@ModelAttribute("command") cmd: Appliable[Seq[ListMarkingWorkflowCommandResult]],
		@PathVariable department: Department
	): Mav = {
		Mav("exams/exams/admin/markingworkflows/list",
			"markingWorkflowInfo" -> cmd.apply(),
			"isExams" -> true
		).crumbs(Breadcrumbs.Exams.Department(
			department,
			retrieveActiveAcademicYear(None).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		))
	}

}

package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{ListReusableWorkflowsCommand, ListReusableWorkflowsState}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear}/markingworkflows"))
class ListReusableMarkingWorkflowController extends CM2MarkingWorkflowController {

	type ListReusableWorkflowsCommand = Appliable[Seq[CM2MarkingWorkflow]] with ListReusableWorkflowsState

	@ModelAttribute("listReusableWorkflowsCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ListReusableWorkflowsCommand(department, academicYear)

	@RequestMapping
	def showForm(
		@ModelAttribute("listReusableWorkflowsCommand") cmd: ListReusableWorkflowsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="actionErrors", required=false) actionErrors: String,
		@RequestParam(value="copiedWorkflow", required=false) copiedWorkflow: CM2MarkingWorkflow,
		@RequestParam(value="deletedWorkflow", required=false) deletedWorkflow: String
	): Mav = {
		// use the SITS rollover date so we can start adding workflows for 'next' year
		val currentAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		commonCrumbs(
			Mav(s"$urlPrefix/admin/workflows/list_reusable", Map(
				"department" -> department,
				"academicYear" -> academicYear,
				"workflows" -> cmd.apply(),
				"currentYear" -> currentAcademicYear,
				"isCurrentYear" -> (currentAcademicYear == academicYear),
				"actionErrors" -> actionErrors,
				"copiedWorkflow" -> copiedWorkflow,
				"deletedWorkflow" -> deletedWorkflow
			)),
			department,
			academicYear
		)

	}

}

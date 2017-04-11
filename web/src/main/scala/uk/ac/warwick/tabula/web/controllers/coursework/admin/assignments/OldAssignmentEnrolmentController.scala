package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.web.Mav


/**
 * Controller to populate the user listing for editing, without persistence
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/enrolment/{academicYear}"))
class OldAssignmentEnrolmentController extends OldCourseworkController with Logging{

	validatesSelf[EditAssignmentEnrolmentCommand]

	@ModelAttribute def formObject(@PathVariable module: Module, @PathVariable academicYear: AcademicYear): EditAssignmentEnrolmentCommand = {
		val cmd = new EditAssignmentEnrolmentCommand(mandatory(module), academicYear)
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(form: EditAssignmentEnrolmentCommand, openDetails: Boolean = false): Mav = {
		form.afterBind()

		logger.info(s"Assignment Enrolment includeCount: ${form.membershipInfo.includeCount}")
		Mav(s"$urlPrefix/admin/assignments/enrolment",
			"department" -> form.module.adminDepartment,
			"module" -> form.module,
			"academicYear" -> form.academicYear,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"openDetails" -> openDetails)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

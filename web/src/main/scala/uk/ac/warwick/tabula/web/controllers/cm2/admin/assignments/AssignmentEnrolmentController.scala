package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController


/**
	* Controller to populate the user listing for editing, without persistence
	*/
@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/enrolment"))
class AssignmentEnrolmentController extends CourseworkController {

	validatesSelf[SelfValidating]

	type EditAssignmentMembershipCommand = Appliable[Assignment] with ModifiesAssignmentMembership

	@ModelAttribute("command")
	def formObject(@PathVariable assignment: Assignment) = {
		val cmd = EditAssignmentMembershipCommand.stub(mandatory(assignment))
		cmd.upstreamGroups.clear()
		cmd
	}

	@RequestMapping
	def showForm(@ModelAttribute("command") form: EditAssignmentMembershipCommand, @PathVariable assignment: Assignment): Mav = {
		form.afterBind()
		Mav(s"$urlPrefix/admin/assignments/enrolment",
			"department" -> form.module.adminDepartment,
			"module" -> form.module,
			"academicYear" -> assignment.academicYear,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups)
			.noLayout()
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

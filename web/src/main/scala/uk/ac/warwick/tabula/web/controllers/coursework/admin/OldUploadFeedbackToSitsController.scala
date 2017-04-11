package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.UploadFeedbackToSitsCommand
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Module}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/upload-to-sits"))
class OldUploadFeedbackToSitsController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		UploadFeedbackToSitsCommand(
			mandatory(module),
			mandatory(assignment),
			user,
			GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))
		)

	@RequestMapping(params = Array("!confirm"))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable module: Module): Mav = {
		Mav(s"$urlPrefix/admin/assignments/publish/upload_to_sits",
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		)
	}

	@RequestMapping(method = Array(POST), params = Array("confirm"))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable assignment: Assignment): Mav = {
		cmd.apply()
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

}

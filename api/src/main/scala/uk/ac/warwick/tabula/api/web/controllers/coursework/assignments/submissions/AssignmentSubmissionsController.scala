package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.submissions

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.SubmissionToJsonConverter
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}/submissions"))
class AssignmentSubmissionsController extends ApiController
	with ListSubmissionsForAssignmentApi
	with SubmissionToJsonConverter

trait ListSubmissionsForAssignmentApi {
	self: ApiController with SubmissionToJsonConverter =>

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@Valid @ModelAttribute("listCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"submissions" -> results.students.map { student =>
					student.user.getWarwickId -> jsonSubmissionObject(student)
				}.toMap
			)))
		}
	}
}
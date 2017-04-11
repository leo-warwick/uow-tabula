package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.StudentAssignmentInfoToJsonConverter
import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkFullScreenCommand
import uk.ac.warwick.tabula.data.model.Member
import MemberAssignmentsController._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

object MemberAssignmentsController {
	type ViewMemberAssignmentsCommand = Appliable[StudentAssignments]
}

@Controller
@RequestMapping(Array("/v1/member/{member}/assignments"))
class MemberAssignmentsController
	extends ApiController
		with GetMemberAssignmentsApi
		with StudentAssignmentInfoToJsonConverter

trait GetMemberAssignmentsApi {
	self: ApiController with StudentAssignmentInfoToJsonConverter =>

	@ModelAttribute("getAssignmentsCommand")
	def command(@PathVariable member: Member): ViewMemberAssignmentsCommand =
		StudentCourseworkFullScreenCommand(MemberOrUser(member))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("getAssignmentsCommand") command: ViewMemberAssignmentsCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val info = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"enrolledAssignments" -> info.enrolledAssignments.map(jsonAssignmentInfoObject),
				"historicAssignments" -> info.historicAssignments.map(jsonAssignmentInfoObject)
			)))
		}
	}
}

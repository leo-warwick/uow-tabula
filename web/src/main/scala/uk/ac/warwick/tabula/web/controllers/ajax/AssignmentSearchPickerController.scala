package uk.ac.warwick.tabula.web.controllers.ajax

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, Command, CommandInternal, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/ajax/assignmentpicker/query"))
class AssignmentSearchPickerController extends BaseController {

	@ModelAttribute("command")
	def command = AssignmentSearchPickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command")cmd: Appliable[Seq[Assignment]]): Mav = {
		val assignments = cmd.apply()
		Mav(
			new JSONView(
				assignments.map(assignment => Map(
					"id" -> assignment.id,
					"name" -> assignment.name,
					"module" -> assignment.module.code,
					"department" -> assignment.module.adminDepartment.name
				))
			)
		)
	}

}

class AssignmentSearchPickerCommand extends CommandInternal[Seq[Assignment]] {

	self: AssessmentServiceComponent =>

	var query: String = _

	def applyInternal(): Seq[Assignment] = {
		if (query.isEmptyOrWhitespace) {
			Seq()
		} else {
			assessmentService.findAssignmentsByNameOrModule(query)
		}
	}

}

object AssignmentSearchPickerCommand {
	def apply() = new AssignmentSearchPickerCommand with Command[Seq[Assignment]] with AutowiringAssessmentServiceComponent
	with ReadOnly with Unaudited with Public
}

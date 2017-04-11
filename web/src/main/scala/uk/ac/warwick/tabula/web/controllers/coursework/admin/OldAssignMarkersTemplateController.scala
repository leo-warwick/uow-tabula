package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.AssignMarkersTemplateCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Exam}
import javax.validation.Valid

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.web.views.ExcelView

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/assign-markers/template"))
class OldAssignMarkersTemplateController {


	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = AssignMarkersTemplateCommand(assignment)

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: Appliable[ExcelView]): ExcelView = {
		cmd.apply()
	}

}

package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{SearchAssignmentCommandState, SearchAssignmentsCommand}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConversions._

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/{module}/assignments"))
class AssignmentSearchController extends CourseworkController {
	type SearchAssignmentsCommand = Appliable[Seq[Assignment]] with SearchAssignmentCommandState

	@ModelAttribute("searchAssignmentCommand")
	def searchAssignmentCommand(@PathVariable module: Module): SearchAssignmentsCommand =
		SearchAssignmentsCommand(mandatory(module))

	@RequestMapping(method = Array(GET), produces = Array("application/json"), params = Array("query"))
	def submitSearchJSON(@Valid @ModelAttribute("searchAssignmentCommand") cmd: SearchAssignmentsCommand): Mav = {
		val assignmentsJson: JList[Map[String, Object]] = toJson(cmd.apply())
		Mav(new JSONView(assignmentsJson))
	}

	def toJson(assignments: Seq[Assignment]): Seq[Map[String, String]] = {

		def assignmentToJson(assignment: Assignment) = Map[String, String](
			"name" -> assignment.name,
			"id" -> assignment.id,
			"moduleName" -> assignment.module.name,
			"moduleCode" -> assignment.module.code)

		assignments.map(assignmentToJson)
	}

}
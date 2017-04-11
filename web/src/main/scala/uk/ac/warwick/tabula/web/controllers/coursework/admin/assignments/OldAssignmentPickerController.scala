package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.permissions._

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/picker"))
class OldAssignmentPickerController extends OldCourseworkController {
	@Autowired var json: ObjectMapper = _

	@ModelAttribute def command(@PathVariable module: Module) = new AssignmentPickerCommand(module)

	@RequestMapping
	def submit(cmd: AssignmentPickerCommand): JSONView = {
		val assignmentsJson: JList[Map[String, Object]] = toJson(cmd.apply())

		new JSONView(assignmentsJson)
	}

	def toJson(assignments: Seq[Assignment]): Seq[Map[String, String]] = {

		def assignmentToJson(assignment: Assignment) = Map[String, String](
			"name" -> assignment.name,
			"id" -> assignment.id,
			"moduleName" -> assignment.module.name,
			"moduleCode" -> assignment.module.code)

		val assignmentsJson = assignments.map(assignmentToJson)
		assignmentsJson
	}

}

class AssignmentPickerCommand(module: Module) extends Command[Seq[Assignment]] with ReadOnly with Unaudited {
	PermissionCheck(Permissions.Assignment.Read, module)

	var assignmentService: AssessmentService = Wire.auto[AssessmentService]

	var searchTerm: String = ""

	def applyInternal(): Seq[Assignment] = assignmentService.getAssignmentsByName(searchTerm, module.adminDepartment)
}

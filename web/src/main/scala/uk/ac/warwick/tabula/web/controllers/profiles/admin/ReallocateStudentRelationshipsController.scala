package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships._
import uk.ac.warwick.tabula.commands.{Appliable, StudentAssociationResult}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/{relationshipType}/reallocate/{agentId}"))
class ReallocateStudentRelationshipsController extends ProfilesController {

	@ModelAttribute("activeDepartment")
	def activeDepartment(@PathVariable department: Department): Department = department

	@ModelAttribute("commandActions")
	def commandActions = FetchDepartmentRelationshipInformationCommand.Actions

	@ModelAttribute("allocationTypes")
	def allocationTypes = ExtractRelationshipsFromFileCommand.AllocationTypes

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable agentId: String
	) =
		FetchDepartmentRelationshipsForReallocationCommand(mandatory(department), mandatory(relationshipType), agentId)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[StudentAssociationResult], @PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType): Mav = {
		val results = cmd.apply()
		if (ajax) {
			Mav(new JSONView(
				Map("unallocated" -> results.unallocated.map(studentData => Map(
					"firstName" -> studentData.firstName,
					"lastName" -> studentData.lastName,
					"universityId" -> studentData.universityId
				)))
			)).noLayout()
		} else {
			Mav("profiles/relationships/reallocate",
				"unallocated" -> results.unallocated,
				"allocated" -> results.allocated
			)
		}
	}

}
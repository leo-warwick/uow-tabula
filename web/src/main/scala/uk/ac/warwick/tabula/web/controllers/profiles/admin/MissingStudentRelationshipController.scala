package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.relationships.MissingStudentRelationshipsCommand
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}/missing"))
class MissingStudentRelationshipController extends ProfilesController {

	@ModelAttribute("missingStudentRelationshipCommand")
	def missingStudentRelationshipCommand(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) =	MissingStudentRelationshipsCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(
		@PathVariable department: Department,
		@ModelAttribute("missingStudentRelationshipCommand") missing: Appliable[(Int, Seq[Member])]
	): Mav = {
		val (studentCount, missingStudents) = missing.apply()
		Mav("profiles/relationships/missing_agent_view",
			"studentCount" -> studentCount,
			"missingStudents" -> missingStudents,
			"department" -> department
		)
	}
}

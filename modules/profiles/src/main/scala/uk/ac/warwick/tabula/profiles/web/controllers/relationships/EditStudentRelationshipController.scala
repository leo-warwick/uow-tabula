package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.profiles.commands.relationships.EditStudentRelationshipCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/{relationshipType}/{studentCourseDetails}"))
class EditStudentRelationshipController extends BaseController {
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("editStudentRelationshipCommand")
	def editStudentRelationshipCommand(
			@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
			@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
			@RequestParam(value="currentAgent", required=false) currentAgent: Member,
			@RequestParam(value="remove", required=false) remove: Boolean,
			user: CurrentUser
			) =
		new EditStudentRelationshipCommand(studentCourseDetails, relationshipType, Option(currentAgent), user, Option(remove).getOrElse(false))

	// initial form display
	@RequestMapping(value = Array("/edit","/add"),method=Array(GET))
	def editAgent(@ModelAttribute("editStudentRelationshipCommand") cmd: EditStudentRelationshipCommand, request: HttpServletRequest) = {
		Mav("relationships/edit/view",
			"studentCourseDetails" -> cmd.studentCourseDetails,
			"agentToDisplay" -> cmd.currentAgent
		).noLayout()
	}

	@RequestMapping(value = Array("/edit", "/add"), method=Array(POST))
	def saveAgent(@ModelAttribute("editStudentRelationshipCommand") cmd: EditStudentRelationshipCommand, request: HttpServletRequest ) = {
		cmd.apply()

		Mav("relationships/edit/view",
			"student" -> cmd.studentCourseDetails.student,
			"agentToDisplay" -> cmd.currentAgent
		)
	}
}

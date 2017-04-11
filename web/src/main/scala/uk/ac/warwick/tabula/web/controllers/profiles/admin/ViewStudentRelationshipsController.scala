package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.ViewRelatedStudentsCommand
import uk.ac.warwick.tabula.commands.profiles.relationships.ViewStudentRelationshipsCommand
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}"))
class ViewStudentRelationshipsController extends ProfilesController {

	@ModelAttribute("viewStudentRelationshipsCommand")
	def viewStudentRelationshipsCommand(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) = ViewStudentRelationshipsCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType,
		@ModelAttribute("viewStudentRelationshipsCommand") command: Appliable[ViewStudentRelationshipsCommand.Result]
	): Mav = {
		val agentGraph = command.apply()
		Mav("profiles/relationships/agent_view",
			"agentRelationships" -> agentGraph.studentMap,
			"missingCount" -> agentGraph.missingCount,
			"scheduledCount" -> agentGraph.scheduledCount,
			"courseMap" -> agentGraph.courseMap,
			"yearOfStudyMap" -> agentGraph.yearOfStudyMap,
			"department" -> department,
			"canReallocateStudents" -> !relationshipType.readOnly(department)
		)
	}
}



@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/students"))
class ViewStudentRelationshipStudentsController extends ProfilesController {

	type ViewRelatedStudentsCommand = ViewRelatedStudentsCommand.CommandType

	@ModelAttribute("viewRelatedStudentsCommand") def command(@PathVariable relationshipType: StudentRelationshipType) =
		ViewRelatedStudentsCommand(currentMember, relationshipType)

	@RequestMapping
	def view(@ModelAttribute("viewRelatedStudentsCommand") viewRelatedStudentsCommand: ViewRelatedStudentsCommand): Mav = {
		val result = viewRelatedStudentsCommand.apply()
		val studentCourseDetails = result.entities
		val students = studentCourseDetails.map(_.student).distinct.sortBy { student =>  (student.lastName, student.firstName) }
		val meetingInfoMap = result.lastMeetingWithTotalPendingApprovalsMap

		if(ajax)
			Mav("profiles/relationships/student_view_results",
				"studentCourseDetails" -> studentCourseDetails,
				"students" -> students,
				"meetingsMap" -> meetingInfoMap
			).noLayout()
		else
			Mav("profiles/relationships/student_view",
				"studentCourseDetails" -> studentCourseDetails,
				"students" -> students,
				"meetingsMap" -> meetingInfoMap
			)
	}
}

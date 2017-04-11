package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.relationships.ScheduledStudentRelationshipChangesCommand
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}/scheduled"))
class ScheduledStudentRelationshipChangesController extends ProfilesController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) =	ScheduledStudentRelationshipChangesCommand(department, relationshipType)

	@ModelAttribute("UpdateScheduledStudentRelationshipChangesControllerActions")
	def actions = UpdateScheduledStudentRelationshipChangesControllerActions

	@RequestMapping(method = Array(HEAD, GET))
	def view(
		@PathVariable department: Department,
		@ModelAttribute("command") cmd: Appliable[Map[DateTime, Seq[StudentRelationship]]]
	): Mav = {
		val result = cmd.apply().toSeq.sortBy { case (dateTime, _) => dateTime }
		val groupedByDate = result.groupBy { case (dateTime, _) => dateTime.toLocalDate }
			.toSeq.sortBy { case (date, _) => date }

		Mav("profiles/relationships/scheduled_changes", "changesByDate" -> groupedByDate)
	}
}

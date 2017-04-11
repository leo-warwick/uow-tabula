package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.{ComposableCommand, PopulateOnForm}
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.services.AutowiringMeetingRecordServiceComponent

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/schedule/create"))
class CreateScheduledMeetingRecordController extends AbstractManageScheduledMeetingRecordController {

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@RequestParam(value = "relationship", required = false) relationship: StudentRelationship,
		@ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship]
	): CreateScheduledMeetingRecordCommand with ComposableCommand[ScheduledMeetingRecord] with CreateScheduledMeetingPermissions with CreateScheduledMeetingRecordState with CreateScheduledMeetingRecordDescription with AutowiringMeetingRecordServiceComponent with CreateScheduledMeetingRecordCommandValidation with CreateScheduledMeetingRecordNotification with CreateScheduledMeetingRecordScheduledNotifications with PopulateOnForm = {
		allRelationships match {
			case Nil => throw new ItemNotFoundException
			case relationships =>
				// Go through the relationships for this SPR code and find one where the current user is the agent.
				// If there isn't one but there's only one relationship, pick it. Otherwise default to the first.
				val chosenRelationship = relationship match {
					case r: StudentRelationship => r
					case _ => relationships.find(rel => rel.agentMember.map(_.universityId).contains(user.universityId))
						.getOrElse(relationships.head)
				}

				CreateScheduledMeetingRecordCommand(currentMember, chosenRelationship, relationships.size > 1)
		}
	}

}

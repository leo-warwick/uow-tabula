package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{ComposableCommand, PopulateOnForm}
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, _}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/create"))
class CreateMeetingRecordController extends AbstractManageMeetingRecordController {

	@ModelAttribute("command")
	def getCommand(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@RequestParam(value = "relationship", required = false) relationship: StudentRelationship,
		@ModelAttribute("allRelationships") allRelationships: Seq[StudentRelationship]
	): CreateMeetingRecordCommandInternal with AutowiringMeetingRecordServiceComponent with AutowiringFeaturesComponent with AutowiringAttendanceMonitoringMeetingRecordServiceComponent with AutowiringFileAttachmentServiceComponent with ComposableCommand[MeetingRecord] with MeetingRecordCommandBindListener with ModifyMeetingRecordValidation with CreateMeetingRecordDescription with ModifyMeetingRecordPermissions with CreateMeetingRecordCommandState with MeetingRecordCommandRequest with CreateMeetingRecordCommandNotifications with PopulateOnForm = {
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

				CreateMeetingRecordCommand(currentMember, chosenRelationship)
		}
	}

}

package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent}

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/schedule/{meetingRecord}/edit"))
class EditScheduledMeetingRecordController extends AbstractManageScheduledMeetingRecordController {

	@ModelAttribute("command")
	def getCommand(@PathVariable meetingRecord: ScheduledMeetingRecord): EditScheduledMeetingRecordCommand with ComposableCommand[ScheduledMeetingRecordResult] with EditScheduledMeetingRecordPermissions with EditScheduledMeetingRecordState with EditScheduledMeetingRecordDescription with AutowiringMeetingRecordServiceComponent with EditScheduledMeetingRecordCommandValidation with EditScheduledMeetingRecordNotification with AutowiringFileAttachmentServiceComponent with EditScheduledMeetingRecordNotifications with PopulateScheduledMeetingRecordCommand =  {
		EditScheduledMeetingRecordCommand(currentMember, meetingRecord)
	}

}

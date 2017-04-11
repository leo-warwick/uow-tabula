package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings.EditMeetingRecordCommand
import uk.ac.warwick.tabula.data.model._

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/{academicYear}/edit/{meetingRecord}"))
class EditMeetingRecordController extends AbstractManageMeetingRecordController {

	@ModelAttribute("command")
	def getCommand(@PathVariable meetingRecord: MeetingRecord) =
		EditMeetingRecordCommand(mandatory(meetingRecord))
}

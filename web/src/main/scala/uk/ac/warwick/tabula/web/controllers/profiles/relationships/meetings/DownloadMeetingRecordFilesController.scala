package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMethod}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings.DownloadMeetingRecordFilesCommand
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
class DownloadMeetingRecordFilesController extends BaseController {

	@ModelAttribute def command(@PathVariable meetingRecord: AbstractMeetingRecord)
		= new DownloadMeetingRecordFilesCommand(meetingRecord)

	// the difference between the RequestMapping paths for these two methods is a bit subtle - the first has
	// attachments plural, the second has attachments singular.
	@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/attachments/*"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAll(command: DownloadMeetingRecordFilesCommand): RenderableFile = {
		getOne(command, null)
	}

	@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getOne(command: DownloadMeetingRecordFilesCommand, @PathVariable filename: String): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}
}

package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMethod}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.profiles.DownloadMemberNoteFilesCommand
import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
class DownloadMemberNoteFilesController extends BaseController {

	@ModelAttribute def command(@PathVariable memberNote: MemberNote)
	= new DownloadMemberNoteFilesCommand(memberNote)

	// the difference between the RequestMapping paths for these two methods is a bit subtle - the first has
	// attachments plural, the second has attachments singular.
	@RequestMapping(value = Array("/profiles/notes/{memberNote}/attachments/*"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAll(command: DownloadMemberNoteFilesCommand): RenderableFile = {
		getOne(command, null)
	}

	@RequestMapping(value = Array("/profiles/notes/{memberNote}/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getOne(command: DownloadMemberNoteFilesCommand, @PathVariable filename: String): RenderableFile = {
		command.apply().getOrElse {
			throw new ItemNotFoundException()
		}
	}
}

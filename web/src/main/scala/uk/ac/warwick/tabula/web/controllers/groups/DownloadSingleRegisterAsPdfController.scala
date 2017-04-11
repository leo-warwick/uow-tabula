package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.DownloadSingleRegisterAsPdfCommand
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav

@RequestMapping(value = Array("/groups/event/{event}/register.pdf"))
@Controller
class DownloadSingleRegisterAsPdfController extends GroupsController {

	type DownloadRegisterAsPdfCommand = Appliable[RenderableFile]

	@ModelAttribute
	def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int): DownloadRegisterAsPdfCommand
		= DownloadSingleRegisterAsPdfCommand(mandatory(event), mandatory(week), s"register-week$week.pdf", user)

	@RequestMapping
	def downloadAsPdf(@ModelAttribute command: DownloadRegisterAsPdfCommand): Mav = {
		Mav(new RenderableFileView(command.apply()))
	}

}

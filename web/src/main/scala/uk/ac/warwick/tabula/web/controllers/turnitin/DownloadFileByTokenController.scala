package uk.ac.warwick.tabula.web.controllers.turnitin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.scheduling.turnitin._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import javax.validation.Valid

import uk.ac.warwick.tabula.services.AutowiringOriginalityReportServiceComponent
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

@Controller
@RequestMapping(value = Array("/turnitin/submission/{submission}/attachment/{fileAttachment}"))
class DownloadFileByTokenController extends BaseController with Logging {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable submission: Submission,
		@PathVariable fileAttachment: FileAttachment,
		@RequestParam(value="token", required=true) token: FileAttachmentToken): DownloadFileByTokenCommandInternal with ComposableCommand[RenderableFile] with AutowiringOriginalityReportServiceComponent with DownloadFileByTokenCommandState with DownloadFileByTokenValidation with DownloadFileByTokenDescription with PubliclyVisiblePermissions =	{
			mustBeLinked(mandatory(fileAttachment), mandatory(submission))
			DownloadFileByTokenCommand(submission, fileAttachment, mandatory(token))
	}

	@RequestMapping(method = Array(GET))
	def serve(@Valid @ModelAttribute("command") command: Appliable[RenderableFile], errors: Errors) =
		Mav(new RenderableFileView(command.apply()))
}
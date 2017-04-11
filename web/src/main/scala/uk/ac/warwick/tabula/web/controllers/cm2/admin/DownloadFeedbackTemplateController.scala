package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.departments.DownloadFeedbackTemplateCommand
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/settings/feedback-templates/download/{template}/{filename}"))
class DownloadFeedbackTemplateController extends CourseworkController {

	@ModelAttribute def command(
		@PathVariable department: Department,
		@PathVariable template: FeedbackTemplate,
		@PathVariable filename: String,
		user:CurrentUser) =
			new DownloadFeedbackTemplateCommand(mandatory(department), mandatory(template), filename, mandatory(user))

	@RequestMapping(method = Array(GET, HEAD))
	def getAttachment(command: DownloadFeedbackTemplateCommand, user:CurrentUser): RenderableFile = {
		command.apply().getOrElse{ throw new ItemNotFoundException() }
	}

}
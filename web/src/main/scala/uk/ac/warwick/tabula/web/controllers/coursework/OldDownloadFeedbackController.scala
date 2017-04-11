package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.coursework.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}"))
class OldDownloadFeedbackController extends OldCourseworkController {

	var feedbackService: FeedbackService = Wire[FeedbackService]

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser)
		= new DownloadFeedbackCommand(module, assignment, mandatory(feedbackService.getAssignmentFeedbackByUsercode(assignment, user.userId).filter(_.released)), optionalCurrentMember)

	@RequestMapping(value = Array("/all/feedback.zip"))
	def getAll(command: DownloadFeedbackCommand, user: CurrentUser): RenderableFile = {
		command.filename = null
		getOne(command, user)
	}

	@RequestMapping(value = Array("/get/{filename}"))
	def getOne(command: DownloadFeedbackCommand, user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

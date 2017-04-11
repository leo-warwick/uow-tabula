package uk.ac.warwick.tabula.web.controllers.coursework.admin.modules

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.coursework.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{Assignment, Member, Module}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/{student}"))
class OldDownloadFeedbackInProfileController extends OldCourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable student: Member)
		= new DownloadFeedbackCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedbackByUsercode(assignment, student.userId).filter(_.released)), Some(student))

	@RequestMapping(value = Array("/all/feedback.zip"))
	def getAll(command: DownloadFeedbackCommand, @PathVariable student: Member): RenderableFile = {
		command.filename = null
		getOne(command)
	}

	@RequestMapping(value = Array("/get/{filename}"))
	def getOne(command: DownloadFeedbackCommand): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

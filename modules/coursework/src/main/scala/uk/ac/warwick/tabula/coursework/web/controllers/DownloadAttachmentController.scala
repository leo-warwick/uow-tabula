package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.fileserver.FileServer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadAttachmentCommand
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.coursework.commands.feedback.DownloadFeedbackCommand
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.ItemNotFoundException
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}"))
class DownloadAttachmentController extends CourseworkController {
	
	var assignmentService = Wire.auto[AssignmentService]

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) 
		= new DownloadAttachmentCommand(module, assignment, mandatory(assignmentService.getSubmissionByUniId(assignment, user.universityId)))

	@Autowired var fileServer: FileServer = _

	@RequestMapping(value = Array("/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: DownloadAttachmentCommand, user: CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable) }
		command.apply().orElse { throw new ItemNotFoundException() }
	}

}
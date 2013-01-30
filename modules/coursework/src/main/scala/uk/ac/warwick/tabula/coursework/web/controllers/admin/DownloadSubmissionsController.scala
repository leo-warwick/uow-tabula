package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.commands.assignments.{DownloadFeedbackSheetsCommand, DownloadAllSubmissionsCommand, DownloadSubmissionsCommand}
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{UserLookupService, AssignmentService}
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.coursework.commands.assignments.AdminGetSingleSubmissionCommand
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadMarkersSubmissionsCommand

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
class DownloadSubmissionsController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]

	@ModelAttribute def getSingleSubmissionCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new DownloadSubmissionsCommand(module, assignment)

	@RequestMapping
	def download(command: DownloadSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {		
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}
	
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/submissions.zip"))
class DownloadMarkerSubmissionsController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]
	
	@ModelAttribute def getMarkersSubmissionCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) =
		new DownloadMarkersSubmissionsCommand(module, assignment, user)

	@RequestMapping
	def downloadMarkersSubmissions(command: DownloadMarkersSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}
	
}
	
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
class DownloadAllSubmissionsController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]
	
	@ModelAttribute def getAllSubmissionsSubmissionCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, @PathVariable("filename") filename: String) = 
		new DownloadAllSubmissionsCommand(module, assignment, filename)

	@RequestMapping
	def downloadAll(command: DownloadAllSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}
	
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download/{submissionId}/{filename}"))
class DownloadSingleSubmissionController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]
	var assignmentService = Wire.auto[AssignmentService]
	
	@ModelAttribute def getSingleSubmissionCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, @PathVariable("submissionId") submissionId: String) = 
		new AdminGetSingleSubmissionCommand(module, assignment, mandatory(assignmentService.getSubmission(submissionId)))

	@RequestMapping
    def downloadSingle(cmd: AdminGetSingleSubmissionCommand, @PathVariable("filename") filename: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		fileServer.serve(cmd.apply())
    }
	
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}"))
class DownloadFeedbackSheetsController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]
	var userLookup = Wire.auto[UserLookupService]
		
	@ModelAttribute def feedbackSheetsCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new DownloadFeedbackSheetsCommand(module, assignment)

	@RequestMapping(value = Array("/feedback-templates.zip"))
	def downloadFeedbackTemplatesOnly(command: DownloadFeedbackSheetsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val assignment = command.assignment
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

	@RequestMapping(value = Array("/marker-templates.zip"))
	def downloadMarkerFeedbackTemplates(command: DownloadFeedbackSheetsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val assignment = command.assignment

		val submissions = assignment.getMarkersSubmissions(user.apparentUser).getOrElse(
			throw new IllegalStateException("Cannot download submissions for assignments with no mark schemes")
		)

		val users = submissions.map(s => userLookup.getUserByUserId(s.userId))
		command.members = users
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

}

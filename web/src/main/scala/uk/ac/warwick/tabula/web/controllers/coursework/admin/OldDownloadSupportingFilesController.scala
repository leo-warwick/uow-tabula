package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadSupportingFilesCommand
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/extension"))
class OldDownloadSupportingFilesController extends OldCourseworkController {

	@ModelAttribute def command(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable filename: String,
			user: CurrentUser) =
		new DownloadSupportingFilesCommand(module, assignment, mandatory(assignment.findExtension(user.universityId)), filename)

	@RequestMapping(value=Array("/supporting-file/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command: DownloadSupportingFilesCommand, user: CurrentUser): RenderableFile = {
		command.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/extensions/review-request/{universityId}"))
class OldAdminDownloadSupportingFilesController extends OldCourseworkController {

	@ModelAttribute def command(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable filename: String,
			@PathVariable universityId: String) =
		new DownloadSupportingFilesCommand(module, assignment, mandatory(assignment.findExtension(universityId)), filename)

	@RequestMapping(value=Array("/supporting-file/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command:DownloadSupportingFilesCommand, user:CurrentUser, @PathVariable filename: String): RenderableFile = {
		command.apply().getOrElse{ throw new ItemNotFoundException() }
	}

}
package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{AdminGetSingleSubmissionCommand, DownloadAllSubmissionsCommand, DownloadAttachmentCommand, DownloadFeedbackSheetsCommand, DownloadMarkersSubmissionsCommand, DownloadSubmissionsCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User


@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions.zip"))
class OldDownloadSubmissionsController extends OldCourseworkController {

	@ModelAttribute("command")
	def getSingleSubmissionCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadSubmissionsCommand(module, assignment, user)

	@RequestMapping
	def download(@ModelAttribute("command") command: DownloadSubmissionsCommand, @PathVariable assignment: Assignment): Mav = {
		command.apply() match {
			case Left(renderable) =>
				Mav(new RenderableFileView(renderable))
			case Right(jobInstance) =>
				Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/submissions.zip"))
class OldDownloadMarkerSubmissionsController extends OldCourseworkController {

	@ModelAttribute("command")
	def getMarkersSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable marker: User,
			submitter: CurrentUser
	) =	DownloadMarkersSubmissionsCommand(module, assignment, marker, submitter)

	@RequestMapping
	def downloadMarkersSubmissions(@ModelAttribute("command") command: Appliable[RenderableFile]): RenderableFile = {
		command.apply()
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/submissions.zip"))
class OldDownloadMarkerSubmissionsControllerCurrentUser extends OldCourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.submissions(assignment, currentUser.apparentUser))
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
class OldDownloadAllSubmissionsController extends OldCourseworkController {

	@ModelAttribute def getAllSubmissionsSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable filename: String) =
		new DownloadAllSubmissionsCommand(module, assignment, filename)

	@RequestMapping
	def downloadAll(command: DownloadAllSubmissionsCommand): RenderableFile = {
		command.apply()
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(
	value = Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions/download/{submission}/{filename}.zip"),
	params = Array("!single"))
class OldDownloadSingleSubmissionController extends OldCourseworkController {

	var userLookup: UserLookupService = Wire[UserLookupService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission) =
		new AdminGetSingleSubmissionCommand(module, assignment, mandatory(submission))

	@RequestMapping
	def downloadSingle(cmd: AdminGetSingleSubmissionCommand): RenderableFile = cmd.apply()
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions/download/{submission}/{filename}"))
class OldDownloadSingleSubmissionFileController extends OldCourseworkController {
	var userLookup: UserLookupService = Wire[UserLookupService]
	var profileService: ProfileService = Wire.auto[ProfileService]

	@ModelAttribute def getSingleSubmissionCommand(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@PathVariable submission: Submission ): DownloadAttachmentCommand = {
		val student = profileService.getMemberByUser(userLookup.getUserByUserId(mandatory(submission).usercode))
		new DownloadAttachmentCommand(module, assignment, mandatory(submission), student)
	}

	@RequestMapping
	def downloadSingle(cmd: DownloadAttachmentCommand): RenderableFile = {
		val file = cmd.apply()
		file.getOrElse { throw new ItemNotFoundException() }
	}

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}"))
class OldDownloadFeedbackSheetsController extends OldCourseworkController {

	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	@ModelAttribute def feedbackSheetsCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadFeedbackSheetsCommand(module, assignment)

	@RequestMapping(value = Array("/feedback-templates.zip"))
	def downloadFeedbackTemplatesOnly(command: DownloadFeedbackSheetsCommand): RenderableFile = {
		command.apply()
	}

	@RequestMapping(value = Array("/marker-templates.zip"))
	def downloadMarkerFeedbackTemplates(command: DownloadFeedbackSheetsCommand): RenderableFile = {
		val assignment = command.assignment

		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val users = submissions.map(s => userLookup.getUserByUserId(s.usercode))
		command.members = users
		command.apply()
	}

}

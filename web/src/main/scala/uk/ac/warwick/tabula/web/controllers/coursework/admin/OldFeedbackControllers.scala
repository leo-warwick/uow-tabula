package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, ReadOnly}
import uk.ac.warwick.tabula.commands.coursework.feedback._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringZipServiceComponent
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.RenderableFileView
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}.zip"))
class OldDownloadSelectedFeedbackController extends OldCourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute
	def singleFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) = new AdminGetSingleFeedbackCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: AdminGetSingleFeedbackCommand, @PathVariable filename: String): Mav = {
		Mav(new RenderableFileView(cmd.apply()))
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class OldDownloadSelectedFeedbackFileController extends OldCourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def singleFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String
	) =
		new AdminGetSingleFeedbackFileCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedback(feedbackId)))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: AdminGetSingleFeedbackFileCommand, @PathVariable filename: String): Mav = {
		val renderable = cmd.apply().getOrElse {
			throw new ItemNotFoundException()
		}
		Mav(new RenderableFileView(renderable))
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback.zip"))
class OldDownloadAllFeedbackController extends OldCourseworkController {

	@ModelAttribute("command")
	def selectedFeedbacksCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		new DownloadSelectedFeedbackCommand(module, assignment, user)

	@RequestMapping
	def getSelected(@ModelAttribute("command") command: DownloadSelectedFeedbackCommand, @PathVariable assignment: Assignment): Mav = {
		command.apply() match {
			case Left(renderable) =>
				Mav(new RenderableFileView(renderable))
			case Right(jobInstance) =>
				Redirect(Routes.zipFileJob(jobInstance), "returnTo" -> Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{feedbackId}/{filename}"))
class OldDownloadMarkerFeedbackController extends OldCourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	@RequestMapping
	def markerFeedback(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable feedbackId: String,
		@PathVariable filename: String,
		@PathVariable marker: User
	): Mav = {
		feedbackDao.getMarkerFeedback(feedbackId) match {
			case Some(markerFeedback) =>
				val renderable = new AdminGetSingleMarkerFeedbackCommand(module, assignment, markerFeedback).apply()
				Mav(new RenderableFileView(renderable))
			case None => throw new ItemNotFoundException
		}
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{feedbackId}/{filename}"))
class OldDownloadMarkerFeedbackControllerCurrentUser extends OldCourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, @PathVariable feedbackId: String, @PathVariable filename: String, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.marker(assignment, currentUser.apparentUser, feedbackId, filename))
	}
}

@Profile(Array("cm1Enabled")) @Controller
class OldDownloadMarkerFeebackFilesController extends BaseController {

	@ModelAttribute def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable markerFeedback: String,
		@PathVariable marker: User
	) = new DownloadMarkerFeedbackFilesCommand(module, assignment, markerFeedback)

	// the difference between the RequestMapping paths for these two methods is a bit subtle - the first has
	// attachments plural, the second has attachments singular.
	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{markerFeedback}/attachments/*"))
	def getAll(command: DownloadMarkerFeedbackFilesCommand): Mav = {
		getOne(command, null)
	}

	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{markerFeedback}/attachments/*"))
	def redirectAll(@PathVariable assignment: Assignment, @PathVariable markerFeedback: String, @PathVariable marker: User): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.all(assignment, marker, markerFeedback))
	}

	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/download/{markerFeedback}/attachment/{filename}"))
	def getOne(command: DownloadMarkerFeedbackFilesCommand, @PathVariable filename: String): Mav = {
		val renderable = command.apply().getOrElse {
			throw new ItemNotFoundException()
		}
		Mav(new RenderableFileView(renderable))
	}

	@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/feedback/download/{markerFeedback}/attachment/{filename}"))
	def redirectOne(@PathVariable assignment: Assignment, @PathVariable markerFeedback: String, @PathVariable marker: User, @PathVariable filename: String) {
		Redirect(Routes.admin.assignment.markerFeedback.downloadFeedback.one(assignment, marker, markerFeedback, filename))
	}
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/marker/{marker}/{position}/feedback.zip"))
class OldDownloadFirstMarkersFeedbackController extends OldCourseworkController {

	@ModelAttribute("command")
	def downloadFirstMarkersFeedbackCommand(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable position: String,
		@PathVariable marker: User,
		user: CurrentUser
	): DownloadMarkersFeedbackForPositionCommand with ComposableCommand[RenderableFile] with DownloadMarkersFeedbackForPositionDescription with DownloadMarkersFeedbackForPositionPermissions with DownloadMarkersFeedbackForPositionCommandState with ReadOnly with AutowiringZipServiceComponent = {
		val feedbackPosition = position match {
			case "firstmarker" => FirstFeedback
			case "secondmarker" => SecondFeedback
		}
		DownloadMarkersFeedbackForPositionCommand(module, assignment, marker, user, feedbackPosition)
	}

	@RequestMapping
	def getSelected(@ModelAttribute("command") command: Appliable[RenderableFile]): Mav = {
		Mav(new RenderableFileView(command.apply()))
	}
}

// A read only view of all feedback fields and attachments
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/feedback/summary/{student}"))
class OldFeedbackSummaryController extends OldCourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable student: User)
	= FeedbackSummaryCommand(assignment, student)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: Appliable[Option[Feedback]]): Mav = {
		val feedback = command.apply()
		Mav(s"$urlPrefix/admin/assignments/feedback/read_only", "feedback" -> feedback).noLayout()
	}

}
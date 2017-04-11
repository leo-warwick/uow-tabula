package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSubmissionServiceComponent, ProfileServiceComponent, SubmissionServiceComponent}
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/submission-receipt.pdf"))
class DownloadSubmissionReceiptAsPdfController extends CourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(@PathVariable assignment: Assignment, user: CurrentUser): DownloadSubmissionReceiptAsPdfCommand =
		DownloadSubmissionReceiptAsPdfCommand(assignment.module, assignment, user, user.apparentUser)

	@RequestMapping
	def viewAsPdf(command: DownloadSubmissionReceiptAsPdfCommand, user: CurrentUser): PDFView with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent = {
		new PDFView(
			"submission-receipt.pdf",
			s"/WEB-INF/freemarker/$urlPrefix/submit/submission-receipt.ftl",
			Map(
				"submission" -> command.apply()
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent
	}

}

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/submission/{assignment}/{student}/submission-receipt.pdf"))
class DownloadSubmissionReceiptForStudentAsPdfController extends CourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(
		 @PathVariable assignment: Assignment,
		 @PathVariable student: User,
		 user: CurrentUser
	 ): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(assignment.module, assignment, user, student)

	@RequestMapping
	def viewAsPdf(command: DownloadSubmissionReceiptAsPdfCommand, user: CurrentUser): PDFView with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent = {
		new PDFView(
			"submission-receipt.pdf",
			s"/WEB-INF/freemarker/$urlPrefix/submit/submission-receipt.ftl",
			Map(
				"submission" -> command.apply()
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent
	}

}

object DownloadSubmissionReceiptAsPdfCommand {
	val RequiredPermission = Permissions.Submission.Read

	def apply(module: Module, assignment: Assignment, user: CurrentUser, student: User) =
		new DownloadSubmissionReceiptAsPdfCommandInternal(module, assignment, user, student)
			with AutowiringSubmissionServiceComponent
		  with AutowiringProfileServiceComponent
			with DownloadSubmissionReceiptAsPdfPermissions
			with ComposableCommand[Submission]
			with ReadOnly with Unaudited
}

class DownloadSubmissionReceiptAsPdfCommandInternal(val module: Module, val assignment: Assignment, val viewer: CurrentUser, val student: User)
	extends CommandInternal[Submission]
		with DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	override def applyInternal(): Submission = submissionOption.getOrElse(throw new IllegalStateException)
}

trait DownloadSubmissionReceiptAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionReceiptAsPdfState with SubmissionServiceComponent with ProfileServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!viewer.loggedIn) {
			throw new PermissionDeniedException(viewer, DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, assignment)
		}

		notDeleted(mandatory(assignment))
		mustBeLinked(assignment, mandatory(module))

		val submission = mandatory(submissionOption)
		val studentMember = profileService.getMemberByUniversityIdStaleOrFresh(student.getWarwickId)

		mustBeLinked(submission, assignment)
		p.PermissionCheckAny(
			Seq(
				Some(CheckablePermission(Permissions.Submission.Read, submission)),
				studentMember.map(CheckablePermission(Permissions.Submission.Read, _))
			).flatten
		)
	}
}

trait DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def viewer: CurrentUser
	def student: User

	lazy val submissionOption: Option[Submission] = submissionService.getSubmissionByUsercode(assignment, student.getUserId).filter(_.submitted)
}

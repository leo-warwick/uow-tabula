package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.{Assignment, Member, Module, Submission}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, SubmissionServiceComponent}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/submission-receipt.pdf"))
class OldDownloadSubmissionReceiptAsPdfController extends OldCourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		user: CurrentUser
	): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(module, assignment, user, currentMember)

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

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/module/{module}/{assignment}/{studentMember}/submission-receipt.pdf"))
class OldDownloadSubmissionReceiptForStudentAsPdfController extends OldCourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(
		 @PathVariable module: Module,
		 @PathVariable assignment: Assignment,
		 @PathVariable studentMember: Member,
		 user: CurrentUser
	 ): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(module, assignment, user, studentMember)

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

	def apply(module: Module, assignment: Assignment, user: CurrentUser, studentMember: Member) =
		new DownloadSubmissionReceiptAsPdfCommandInternal(module, assignment, user, studentMember)
			with AutowiringSubmissionServiceComponent
			with DownloadSubmissionReceiptAsPdfPermissions
			with ComposableCommand[Submission]
			with ReadOnly with Unaudited
}

class DownloadSubmissionReceiptAsPdfCommandInternal(val module: Module, val assignment: Assignment, val viewer: CurrentUser, val student: Member)
	extends CommandInternal[Submission]
		with DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	override def applyInternal(): Submission = submissionOption.getOrElse(throw new IllegalStateException)
}

trait DownloadSubmissionReceiptAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionReceiptAsPdfState with SubmissionServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!viewer.loggedIn) {
			throw new PermissionDeniedException(viewer, DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, assignment)
		}

		notDeleted(mandatory(assignment))
		mustBeLinked(assignment, mandatory(module))

		val submission = mandatory(submissionOption)

		mustBeLinked(submission, assignment)

		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Submission.Read, submission),
				CheckablePermission(Permissions.Submission.Read, student))
		)
	}
}

trait DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def viewer: CurrentUser
	def student: Member

	lazy val submissionOption: Option[Submission] = submissionService.getSubmissionByUsercode(assignment, student.asSsoUser.getUserId).filter(_.submitted)
}

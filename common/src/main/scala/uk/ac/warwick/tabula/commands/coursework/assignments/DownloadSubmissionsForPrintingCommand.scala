package uk.ac.warwick.tabula.commands.coursework.assignments

import java.io.ByteArrayOutputStream

import com.google.common.io.ByteSource
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module, Submission}
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.pdf.{CombinesPdfs, FreemarkerXHTMLPDFGeneratorComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

trait DownloadAdminSubmissionsForPrintingCommandHelper
	extends FreemarkerXHTMLPDFGeneratorComponent
	with AutowiredTextRendererComponent
	with PhotosWarwickMemberPhotoUrlGeneratorComponent
	with AutowiringFileDaoComponent
	with CombinesPdfs

object DownloadAdminSubmissionsForPrintingCommand {

	final val receiptTemplate = "/WEB-INF/freemarker/coursework/submit/submission-receipt.ftl"
	final val nonPDFTemplate = "/WEB-INF/freemarker/coursework/admin/assignments/submissionsandfeedback/non-pdf-attachments.ftl"
	final val pdfExtension = ".pdf"

	def apply(module: Module, assignment: Assignment) =
		new DownloadSubmissionsForPrintingCommandInternal(module, assignment, null, null)
			with ComposableCommand[RenderableFile]
			with ReadOnly with Unaudited
			with DownloadAdminSubmissionsForPrintingCommandHelper
			with DownloadAdminSubmissionsForPrintingPermissions
			with DownloadSubmissionsForPrintingCommandState
			with DownloadAdminSubmissionsForPrintingCommandRequest
}

object DownloadMarkerSubmissionsForPrintingCommand {

	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser) =
		new DownloadSubmissionsForPrintingCommandInternal(module, assignment, marker, submitter)
			with ComposableCommand[RenderableFile]
			with ReadOnly with Unaudited
			with DownloadAdminSubmissionsForPrintingCommandHelper
			with DownloadMarkerSubmissionsForPrintingPermissions
			with DownloadMarkerSubmissionsForPrintingCommandState
			with DownloadMarkerSubmissionsForPrintingCommandRequest
}


class DownloadSubmissionsForPrintingCommandInternal(val module: Module, val assignment: Assignment, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[RenderableFile] {

	self: DownloadSubmissionsForPrintingCommandRequest with FreemarkerXHTMLPDFGeneratorComponent
		with FileDaoComponent with CombinesPdfs =>

	override def applyInternal(): RenderableAttachment = {
		if (submissions.isEmpty) throw new ItemNotFoundException

		val parts: Seq[FileAttachment] = submissions.flatMap(submission => {
			Seq(
				doReceipt(submission),
				doNonPDFs(submission)
			).flatten ++ getPDFs(submission)
		})

		new RenderableAttachment(combinePdfs(parts, "submissions.pdf"))
	}

	private def doReceipt(submission: Submission): Option[FileAttachment] = {
		val output = new ByteArrayOutputStream()
		pdfGenerator.renderTemplate(
			DownloadAdminSubmissionsForPrintingCommand.receiptTemplate,
			Map(
				"submission" -> submission
			),
			output
		)
		val pdf = new FileAttachment
		pdf.name = s"submission-receipt-${submission.studentIdentifier}.pdf"
		pdf.uploadedData = ByteSource.wrap(output.toByteArray)
		fileDao.saveTemporary(pdf)
		Some(pdf)
	}

	private def doNonPDFs(submission: Submission): Option[FileAttachment] = {
		submission.allAttachments.filter(!_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension)).map(_.name) match {
			case Nil => None
			case attachments =>
				val output = new ByteArrayOutputStream()
				pdfGenerator.renderTemplate(
					DownloadAdminSubmissionsForPrintingCommand.nonPDFTemplate,
					Map(
						"attachments" -> attachments
					),
					output
				)
				val pdf = new FileAttachment
				pdf.name = s"non-pdf-attachments-${submission.studentIdentifier}.pdf"
				pdf.uploadedData = ByteSource.wrap(output.toByteArray)
				fileDao.saveTemporary(pdf)
				Some(pdf)
		}
	}

	private def getPDFs(submission: Submission): Seq[FileAttachment] =
		submission.allAttachments.filter(_.name.endsWith(DownloadAdminSubmissionsForPrintingCommand.pdfExtension))

}

trait DownloadAdminSubmissionsForPrintingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadSubmissionsForPrintingCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Submission.Read, assignment)
	}

}

trait DownloadMarkerSubmissionsForPrintingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadMarkerSubmissionsForPrintingCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Submission.Read, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait DownloadSubmissionsForPrintingCommandState {
	def module: Module
	def assignment: Assignment
}

trait DownloadMarkerSubmissionsForPrintingCommandState extends DownloadSubmissionsForPrintingCommandState {
	def marker: User
	def submitter: CurrentUser
}

trait DownloadSubmissionsForPrintingCommandRequest {

	self: DownloadSubmissionsForPrintingCommandState =>

	var students: JList[String] = JArrayList()

	def submissions: Seq[Submission]
}

trait DownloadAdminSubmissionsForPrintingCommandRequest extends DownloadSubmissionsForPrintingCommandRequest {

	self: DownloadSubmissionsForPrintingCommandState =>

	override def submissions: Seq[Submission] = students.asScala.flatMap(s =>
		assignment.submissions.asScala.find(_.usercode == s)
	)
}

trait DownloadMarkerSubmissionsForPrintingCommandRequest extends DownloadSubmissionsForPrintingCommandRequest {

	self: DownloadMarkerSubmissionsForPrintingCommandState =>

	override def submissions: Seq[Submission] = {
		assignment.getMarkersSubmissions(marker).filter{ submission =>
			val markerFeedback = assignment.getMarkerFeedbackForCurrentPosition(submission.usercode, marker)
			markerFeedback.exists(mf => mf.state != MarkingCompleted)
		}
	}
}

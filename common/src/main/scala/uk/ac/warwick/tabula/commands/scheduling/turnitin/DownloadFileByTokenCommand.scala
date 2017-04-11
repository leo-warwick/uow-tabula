package uk.ac.warwick.tabula.commands.scheduling.turnitin

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions._

object DownloadFileByTokenCommand {
	def apply(submission: Submission, fileAttachment: FileAttachment, token: FileAttachmentToken) =
		new DownloadFileByTokenCommandInternal(submission, fileAttachment, token)
		with ComposableCommand[RenderableFile]
		with AutowiringOriginalityReportServiceComponent
		with DownloadFileByTokenCommandState
		with DownloadFileByTokenValidation
		with DownloadFileByTokenDescription
		with PubliclyVisiblePermissions
}

class DownloadFileByTokenCommandInternal (
		val submission: Submission,
		val fileAttachment: FileAttachment,
		val token: FileAttachmentToken
) extends CommandInternal[RenderableFile] with Logging {

	self: DownloadFileByTokenCommandState with OriginalityReportServiceComponent =>

	override def applyInternal(): RenderableAttachment = {
		val attachment = new RenderableAttachment(fileAttachment)
		token.dateUsed = new DateTime()
		val reportOption = Option(fileAttachment.originalityReport).orElse(
			originalityReportService.getOriginalityReportByFileId(fileAttachment.id)
		)
		reportOption.map { report =>
			report.fileRequested = DateTime.now
			originalityReportService.saveOrUpdate(report)
			report
		}.getOrElse(
			logger.warn(s"Could not find originality report from file attachment ${fileAttachment.id}. This report will have a null fileRequested date.")
		)
		attachment
	}

}

trait DownloadFileByTokenCommandState {
	def submission: Submission
	def fileAttachment: FileAttachment
	def token: FileAttachmentToken

	var fileFound: Boolean = _
}

trait DownloadFileByTokenValidation extends SelfValidating {

	self: DownloadFileByTokenCommandState =>

	override def validate(errors:Errors) {
		if (Option(token.dateUsed).isDefined){
			errors.reject("filedownload.token.used")
		}	else if(token.expires.isBeforeNow){
			errors.reject("filedownload.token.expired")
		} else if (!token.fileAttachmentId.equals(fileAttachment.id)) {
			errors.reject("filedownload.token.invalid")
		}
	}
}

trait DownloadFileByTokenDescription extends Describable[RenderableFile] {

	self: DownloadFileByTokenCommandState =>

	override def describe(d: Description): Unit = {
		d.submission(submission)
		d.fileAttachments(Seq(fileAttachment))
	}

}
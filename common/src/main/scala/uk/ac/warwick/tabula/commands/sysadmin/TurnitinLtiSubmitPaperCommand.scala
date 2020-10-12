package uk.ac.warwick.tabula.commands.sysadmin

import java.net.{MalformedURLException, URL}

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitinlti._
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object TurnitinLtiSubmitPaperCommand {
  def apply(user: CurrentUser) =
    new TurnitinLtiSubmitPaperCommandInternal(user)
      with TurnitinLtiSubmitPaperCommandPermissions
      with ComposableCommand[TurnitinLtiResponse]
      with ReadOnly with Unaudited
      with TurnitinLtiSubmitPaperCommandState
      with TurnitinLtiSubmitPaperValidation
      with AutowiringTurnitinLtiServiceComponent
      with AutowiringOriginalityReportServiceComponent
      with Logging
}

class TurnitinLtiSubmitPaperCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] with Logging {

  self: TurnitinLtiSubmitPaperCommandState with TurnitinLtiServiceComponent with OriginalityReportServiceComponent =>

  override def applyInternal(): TurnitinLtiResponse = transactional() {
    val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email
    val response = turnitinLtiService.submitPaper(assignment, paperUrl, user.userId, userEmail, attachment, user.universityId, "SYSADMIN")

    if (response.success) {
      val originalityReport = originalityReportService.getOriginalityReportByFileId(attachment.id)
      if (originalityReport.isDefined) {
        originalityReport.get.turnitinId = response.turnitinSubmissionId()
        originalityReport.get.reportReceived = false
      } else {
        val report = new OriginalityReport
        report.turnitinId = response.turnitinSubmissionId()
        attachment.originalityReport = report
        originalityReportService.saveOriginalityReport(attachment)
      }
    } else logger.warn("Failed to upload '" + attachment.name + "' - " + response.statusMessage.getOrElse(""))
    response
  }

}

trait TurnitinLtiSubmitPaperCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.ImportSystemData)
  }
}

trait TurnitinLtiSubmitPaperValidation extends SelfValidating {
  self: TurnitinLtiSubmitPaperCommandState =>

  override def validate(errors: Errors): Unit = {
    if (assignment.turnitinId.isEmptyOrWhitespace) {
      errors.rejectValue("assignment", "assignment.turnitinid.empty")
    }
    if (paperUrl.isEmptyOrWhitespace) {
      errors.rejectValue("paperUrl", "turnitin.paperurl.empty")
    } else {
      try {
        val uri = new URL(paperUrl).toURI
        if (!uri.getHost.equals("files.warwick.ac.uk") && (!uri.getHost.equals("tabula.warwick.ac.uk"))) {
          errors.rejectValue("paperUrl", "turnitin.paperurl.invalid")
        }
      }
      catch {
        case e: MalformedURLException =>
          errors.rejectValue("paperUrl", "turnitin.paperurl.invalid")
      }
    }
  }
}

trait TurnitinLtiSubmitPaperCommandState {
  var paperUrl: String = _
  var assignment: Assignment = _
  var attachment: FileAttachment = _
}

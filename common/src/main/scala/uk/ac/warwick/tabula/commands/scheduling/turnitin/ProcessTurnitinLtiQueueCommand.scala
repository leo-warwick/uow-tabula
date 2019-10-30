package uk.ac.warwick.tabula.commands.scheduling.turnitin

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.turnitin.ProcessTurnitinLtiQueueCommand.ProcessTurnitinLtiQueueCommandResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{TurnitinJobErrorNotification, TurnitinJobSuccessNotification}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, OriginalityReport}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitinlti._
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{AutowiringTopLevelUrlComponent, CurrentUser, TopLevelUrlComponent}
import uk.ac.warwick.userlookup.User

object ProcessTurnitinLtiQueueCommand {

  case class ProcessTurnitinLtiQueueCommandResult(
    completedAssignments: Seq[Assignment],
    failedAssignments: Seq[Assignment]
  )

  def apply() =
    new ProcessTurnitinLtiQueueCommandInternal
      with AutowiringTurnitinLtiQueueServiceComponent
      with AutowiringTurnitinLtiServiceComponent
      with AutowiringAssessmentServiceComponent
      with AutowiringFileAttachmentServiceComponent
      with AutowiringOriginalityReportServiceComponent
      with AutowiringTopLevelUrlComponent
      with ComposableCommand[ProcessTurnitinLtiQueueCommandResult]
      with ProcessTurnitinLtiQueuePermissions
      with ProcessTurnitinLtiQueueNotification
      with Unaudited
}

abstract class ProcessTurnitinLtiQueueCommandInternal extends CommandInternal[ProcessTurnitinLtiQueueCommandResult] with Logging {

  self: TurnitinLtiQueueServiceComponent with TurnitinLtiServiceComponent
    with AssessmentServiceComponent with FileAttachmentServiceComponent
    with OriginalityReportServiceComponent with TopLevelUrlComponent =>

  override def applyInternal(): ProcessTurnitinLtiQueueCommandResult = {
    lazy val processedAssignment: Option[Assignment] =
      turnitinLtiQueueService.findAssignmentToProcess.map(processAssignment)
    lazy val processedLongAwaitedReportForReport: Option[OriginalityReport] =
      turnitinLtiQueueService.findReportToProcessForReport(longAwaitedOnly = true).map(processReportForReport)
    lazy val processedReportForSubmission: Option[OriginalityReport] =
      turnitinLtiQueueService.findReportToProcessForSubmission.map(processReportForSubmission)
    lazy val processedReportForReport: Option[OriginalityReport] =
      turnitinLtiQueueService.findReportToProcessForReport(longAwaitedOnly = false).map(processReportForReport)

    processedAssignment.getOrElse(
      processedLongAwaitedReportForReport.getOrElse(
        processedReportForSubmission.getOrElse(
          processedReportForReport
        )
      )
    )

    finish()
  }

  private def userToSubmit(assignment: Assignment): User = {
    assignment.turnitinLtiNotifyUsers.headOption.getOrElse(
      assignment.module.managers.users.headOption.getOrElse(
        assignment.module.adminDepartment.owners.users.headOption.getOrElse {
          Option(assignment.module.adminDepartment).filter(_.hasParent).flatMap(_.rootDepartment.owners.users.headOption).getOrElse {
            val u = new User("tabula")
            u.setFoundUser(true)
            u.setEmail("tabula@warwick.ac.uk")
            u.setFirstName("Tabula")
            u.setLastName("System")
            u
          }
        }
      )
    )
  }

  private def processAssignment(assignment: Assignment): Assignment = {
    logger.info(s"Processing assignment ${assignment.id}...")
    assignment.lastSubmittedToTurnitin = DateTime.now

    val user = userToSubmit(assignment)

    val response = turnitinLtiService.submitAssignment(assignment, new CurrentUser(user, user))
    assignment.submitToTurnitinRetries += 1
    assessmentService.save(assignment)

    if (assignment.submitToTurnitinRetries == TurnitinLtiService.SubmitAssignmentMaxRetries) {
      logger.warn(s"Max retries reached for assignment ${assignment.id}, last response from Turnitin: ${response.html.getOrElse("No html response")}")
    }
    assignment
  }

  private def processReportForSubmission(report: OriginalityReport): OriginalityReport = {
    logger.info(s"Processing report for submission ${report.id}...")

    val attachment = report.attachment
    val submission = attachment.submissionValue.submission
    val assignment = submission.assignment
    // Check for an existing valid token before creating a new one
    val token = fileAttachmentService.getValidToken(attachment).getOrElse {
      // New transaction as Turnitin requests the file while keeping the submitPaper request pending
      transactional(propagation = REQUIRES_NEW) {
        val t = attachment.generateToken()
        fileAttachmentService.saveOrUpdate(t)
        t
      }
    }

    val attachmentUrl = s"$toplevelUrl${Routes.turnitinlti.fileByToken(submission, attachment, token)}"

    val response = turnitinLtiService.submitPaper(
      assignment = assignment,
      paperUrl = attachmentUrl,
      userId = submission.usercode,
      userEmail = s"${submission.usercode}@TurnitinLti.warwick.ac.uk",
      attachment = attachment,
      userFirstName = submission.studentIdentifier,
      userLastName = "Student"
    )

    originalityReportService.refresh(report)

    report.lastSubmittedToTurnitin = DateTime.now

    if (response.success) {
      report.turnitinId = response.turnitinSubmissionId()
    } else {
      report.lastTurnitinError = response.statusMessage.getOrElse("Failed to upload")
      logger.warn(s"Failed to submit paper for report ${report.id}: ${response.statusMessage.getOrElse("Failed to upload")}. ${response.json.orElse(response.html).orElse(response.xml).getOrElse("")}")
      report.submitToTurnitinRetries += 1
    }

    originalityReportService.saveOrUpdate(report)
    report
  }

  private def processReportForReport(report: OriginalityReport): OriginalityReport = {
    logger.info(s"Processing report for report ${report.id}...")
    report.lastReportRequest = DateTime.now

    turnitinLtiService.getSubmissionDetails(report.turnitinId) match {
      case response if response.success && response.submissionInfo().overlap.isDefined =>
        val result = response.submissionInfo()
        report.overlap = result.overlap.map(_.toInt)
        report.publicationOverlap = result.publication_overlap.map(_.toInt)
        report.webOverlap = result.web_overlap.map(_.toInt)
        report.studentOverlap = result.student_overlap.map(_.toInt)
        report.reportReceived = true
      case response =>
        logger.info("Bad response for submission details  - " + response.json.orElse(response.html).orElse(response.xml).getOrElse(""))
        report.lastTurnitinError = response.statusMessage.getOrElse("Failed to retrieve results")
        report.reportRequestRetries += 1
    }

    originalityReportService.saveOrUpdate(report)
    report
  }

  private def finish(): ProcessTurnitinLtiQueueCommandResult = {
    val completedAssignments = turnitinLtiQueueService.listCompletedAssignments
    val failedAssignments = turnitinLtiQueueService.listFailedAssignments

    if (completedAssignments.nonEmpty) logger.info(s"Completed assignments: ${completedAssignments.map(_.id).mkString(", ")}")
    if (failedAssignments.nonEmpty) logger.info(s"Failed assignments: ${failedAssignments.map(_.id).mkString(", ")}")
    (completedAssignments ++ failedAssignments).foreach { a =>
      a.submitToTurnitin = false
      assessmentService.save(a)
    }

    ProcessTurnitinLtiQueueCommandResult(
      completedAssignments,
      failedAssignments
    )
  }

}

trait ProcessTurnitinLtiQueuePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.ImportSystemData)
  }

}

trait ProcessTurnitinLtiQueueNotification extends Notifies[ProcessTurnitinLtiQueueCommandResult, Assignment] {
  self: TurnitinLtiQueueServiceComponent =>

  override def emit(result: ProcessTurnitinLtiQueueCommandResult): Seq[Notification[OriginalityReport, Assignment]] = {
    val completedNotifications = result.completedAssignments.filterNot(_.submissions.isEmpty).flatMap(assignment => {
      val completedReports = turnitinLtiQueueService.listOriginalityReports(assignment).filter(report =>
        report.reportReceived || (!report.reportReceived && (
          report.submitToTurnitinRetries == TurnitinLtiService.SubmitAttachmentMaxRetries ||
            report.reportRequestRetries == TurnitinLtiService.ReportRequestMaxRetries
          ))
      )
      assignment.turnitinLtiNotifyUsers.map(user =>
        Notification.init(new TurnitinJobSuccessNotification, user, completedReports, assignment)
      )
    })

    val failedNotifications = result.failedAssignments.flatMap(assignment => {
      assignment.turnitinLtiNotifyUsers.map(user =>
        Notification.init(new TurnitinJobErrorNotification, user, Seq[OriginalityReport](), assignment)
      )
    })

    completedNotifications ++ failedNotifications
  }
}

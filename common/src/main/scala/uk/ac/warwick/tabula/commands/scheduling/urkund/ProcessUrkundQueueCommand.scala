package uk.ac.warwick.tabula.commands.scheduling.urkund

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.services.urkund._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.util.Success
import scala.util.Failure

object ProcessUrkundQueueCommand {
	
	def apply() =
		new ProcessUrkundQueueCommandInternal
			with AutowiringUrkundServiceComponent
			with AutowiringOriginalityReportServiceComponent
			with ComposableCommand[Option[Assignment]]
			with ProcessUrkundQueuePermissions
			with Unaudited
}


class ProcessUrkundQueueCommandInternal extends CommandInternal[Option[Assignment]] with Logging {

	self: UrkundServiceComponent with OriginalityReportServiceComponent =>

	override def applyInternal(): Option[Assignment] = {
		lazy val processedReportForSubmission = urkundService.findReportToSubmit.map(processReportForSubmission)
		lazy val processedReportForReport = urkundService.findReportToRetreive.map(processReportForReport)

		val processedReport = processedReportForSubmission.orElse(
			processedReportForReport
		)

		processedReport.flatMap(getCompletedAssignment)
	}

	private def processReportForSubmission(report: OriginalityReport): OriginalityReport = {
		logger.info(s"Processing report for Urkund submission ${report.id}...")
		report.submitAttempts = report.submitAttempts + 1

		val response = urkundService.submit(report)
		response match {
			case Success(urkundResponse) => urkundResponse match {
				case success: UrkundSuccessResponse =>
					report.submittedDate = DateTime.now
					report.nextSubmitAttempt = null
					report.nextResponseAttempt = DateTime.now.plusMinutes(UrkundService.reportTimeoutInMinutes)
				case error =>
					report.nextSubmitAttempt = null
					logger.error(s"Client error when submitting report ${report.id} to Urkund: ${error.statusCode}")
			}
			case Failure(e) =>
				logger.error(s"Server error when submitting report ${report.id} to Urkund: ${e.getMessage}")
				UrkundService.setNextSubmitAttempt(report)
		}
		originalityReportService.saveOrUpdate(report)
		report
	}

	private def processReportForReport(report: OriginalityReport): OriginalityReport = {
		logger.info(s"Processing report for Urkund report ${report.id}...")
		report.responseAttempts = report.responseAttempts + 1

		val response = urkundService.retrieveReport(report)
		response match {
			case Success(urkundResponse: UrkundResponse) => urkundResponse match {
				case success: UrkundSuccessResponse =>
					success.status match {
						case UrkundSubmissionStatus.Analyzed =>
							success.report.map(urkundReport => {
								report.reportUrl = urkundReport.reportUrl
								report.significance = urkundReport.significance
								report.matchCount = urkundReport.matchCount
								report.sourceCount = urkundReport.sourceCount
								report.responseReceived = DateTime.now
								report.urkundResponse = success.response
								report.urkundResponseCode = success.statusCode.toString
								report
							}).getOrElse {
								logger.error(s"Report ${report.id} has status ${UrkundSubmissionStatus.Analyzed.status} but no Report was defined")
								report.urkundResponse = success.response
								report.urkundResponseCode = success.statusCode.toString
								report
							}
						case UrkundSubmissionStatus.Rejected =>
							logger.error(s"Report ${report.id} was rejected by Urkund")
							report.urkundResponse = success.response
							report.urkundResponseCode = success.statusCode.toString
						case UrkundSubmissionStatus.Error =>
							logger.error(s"There was an error processing report ${report.id} by Urkund")
							report.urkundResponse = success.response
							report.urkundResponseCode = success.statusCode.toString
						case UrkundSubmissionStatus.Accepted =>
							logger.info(s"Report ${report.id} has been accepted by Urkund, but report is not yet ready (attempt ${report.responseAttempts})")
							report.urkundResponse = success.response
							report.urkundResponseCode = success.statusCode.toString
						case _ =>
							logger.info(s"Report ${report.id} has been submitted to Urkund, but has not yet been accepted (attempt ${report.responseAttempts})")
							report.urkundResponse = success.response
							report.urkundResponseCode = success.statusCode.toString
					}
					success.status match {
						case UrkundSubmissionStatus.Analyzed | UrkundSubmissionStatus.Rejected | UrkundSubmissionStatus.Error =>
							report.nextResponseAttempt = null
						case _ =>
							UrkundService.setNextResponseAttempt(report)
					}
				case error =>
					report.nextResponseAttempt = null
					logger.error(s"Client error when retrieving report ${report.id} from Urkund: ${error.statusCode}")
					report.urkundResponseCode = error.statusCode.toString
			}
			case Failure(e) =>
				logger.error(s"Server error when submitting report ${report.id} to Urkund: ${e.getMessage}")
				UrkundService.setNextResponseAttemptOnError(report)
		}
		originalityReportService.saveOrUpdate(report)
		report
	}

	private def getCompletedAssignment(report: OriginalityReport): Option[Assignment] = {
		if (report.nextResponseAttempt == null && report.nextSubmitAttempt == null) {
			val assignment = report.attachment.submissionValue.submission.assignment
			val reports = urkundService.listOriginalityReports(assignment)
			if (reports.nonEmpty && reports.forall(r => r.nextSubmitAttempt == null && r.nextResponseAttempt == null)) {
				Option(assignment)
			} else {
				None
			}
		} else {
			None
		}
	}
}

trait ProcessUrkundQueuePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

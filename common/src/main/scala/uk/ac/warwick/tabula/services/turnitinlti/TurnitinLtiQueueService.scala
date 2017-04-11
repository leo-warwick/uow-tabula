package uk.ac.warwick.tabula.services.turnitinlti

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}
import uk.ac.warwick.tabula.data.{AutowriringTurnitinLtiQueueDaoComponent, TurnitinLtiQueueDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.urkund.UrkundService
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object TurnitinLtiQueueService {
	case class AssignmentStatus(
		status: String,
		progress: Int,
		finished: Boolean,
		succeeded: Boolean,
		reportReceived: Int,
		reportRequested: Int,
		fileSubmitted: Int,
		awaitingSubmission: Int
	) {
		def toMap: Map[String, Any] = Map(
			"status" -> this.status,
			"progress" -> this.progress,
			"finished" -> this.finished,
			"succeeded" -> this.succeeded,
			"reportReceived" -> this.reportReceived,
			"reportRequested" -> this.reportRequested,
			"fileSubmitted" -> this.fileSubmitted,
			"awaitingSubmission" -> this.awaitingSubmission
		)
	}
}

trait TurnitinLtiQueueService {
	def findAssignmentToProcess: Option[Assignment]
	def findReportToProcessForSubmission: Option[OriginalityReport]
	def findReportToProcessForReport(longAwaitedOnly: Boolean): Option[OriginalityReport]
	def listCompletedAssignments: Seq[Assignment]
	def listFailedAssignments: Seq[Assignment]
	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport]
	def createEmptyOriginalityReports(assignment: Assignment): Seq[OriginalityReport]
	def getAssignmentStatus(assignment: Assignment): TurnitinLtiQueueService.AssignmentStatus
}
	

abstract class AbstractTurnitinLtiQueueService extends TurnitinLtiQueueService with Logging {
	self: TurnitinLtiQueueDaoComponent
		with OriginalityReportServiceComponent
		with FeaturesComponent =>

	def findAssignmentToProcess: Option[Assignment] = {
		turnitinLtiQueueDao.findAssignmentToProcess
	}

	def findReportToProcessForSubmission: Option[OriginalityReport] = {
		turnitinLtiQueueDao.findReportToProcessForSubmission
	}

	def findReportToProcessForReport(longAwaitedOnly: Boolean): Option[OriginalityReport] = {
		turnitinLtiQueueDao.findReportToProcessForReport(longAwaitedOnly)
	}

	def listCompletedAssignments: Seq[Assignment] = {
		turnitinLtiQueueDao.listCompletedAssignments
	}

	def listFailedAssignments: Seq[Assignment] = {
		turnitinLtiQueueDao.listFailedAssignments
	}

	def listOriginalityReports(assignment: Assignment): Seq[OriginalityReport] = {
		turnitinLtiQueueDao.listOriginalityReports(assignment)
	}

	def createEmptyOriginalityReports(assignment: Assignment): Seq[OriginalityReport] = {
		val reports = assignment.submissions.asScala.flatMap(_.allAttachments).filter(attachment =>
			attachment.originalityReport == null &&
				TurnitinLtiService.validFileType(attachment) &&
				TurnitinLtiService.validFileSize(attachment)
		).map(attachment => {
			logger.info(s"Creating blank report for ${attachment.id}")
			val report = new OriginalityReport
			report.attachment = attachment
			attachment.originalityReport = report
			report.lastSubmittedToTurnitin = new DateTime(0)
			report.lastReportRequest = new DateTime(0)
			originalityReportService.saveOrUpdate(report)
			report
		})

		if (features.urkundSubmissions) {
			reports.filter(report =>
				UrkundService.validFileType(report.attachment)
					&& UrkundService.validFileSize(report.attachment)
			).foreach(report => {
				report.nextSubmitAttempt = DateTime.now
				originalityReportService.saveOrUpdate(report)
			})
		}

		reports
	}

	def getAssignmentStatus(assignment: Assignment): TurnitinLtiQueueService.AssignmentStatus = {
		if (!assignment.submitToTurnitin) {
			if (assignment.turnitinId == null) {
				TurnitinLtiQueueService.AssignmentStatus(
					status = "Assignment has not been submitted to Turnitin",
					progress = 100,
					finished = true,
					succeeded = false,
					reportReceived = 0,
					reportRequested = 0,
					fileSubmitted = 0,
					awaitingSubmission = 0
				)
			} else {
				TurnitinLtiQueueService.AssignmentStatus(
					status = "Generated a report",
					progress = 100,
					finished = true,
					succeeded = true,
					reportReceived = 0,
					reportRequested = 0,
					fileSubmitted = 0,
					awaitingSubmission = 0
				)
			}
		} else {
			if (assignment.turnitinId == null) {
				TurnitinLtiQueueService.AssignmentStatus(
					status = "Submitting assignment to Turnitin...",
					progress = 0,
					finished = false,
					succeeded = false,
					reportReceived = 0,
					reportRequested = 0,
					fileSubmitted = 0,
					awaitingSubmission = 0
				)
			} else {
				val reports = listOriginalityReports(assignment)

				val partitionedReports = reports.map(report => {
					if (report.reportReceived) {
						("reportReceived", report)
					} else if (report.fileRequested != null) {
						("reportRequested", report)
					} else if (report.turnitinId != null) {
						("fileSubmitted", report)
					} else {
						("awaitingSubmission", report)
					}
				}).groupBy(_._1).withDefaultValue(Seq())

				val percentComplete = (
					partitionedReports("reportReceived").size * 4 +
					partitionedReports("reportRequested").size * 3 +
					partitionedReports("fileSubmitted").size * 2 +
					partitionedReports("awaitingSubmission").size
				).toFloat / (reports.size * 4).toFloat

				TurnitinLtiQueueService.AssignmentStatus(
					status = "Processing submissions. ",
					progress = (10 + 90 * percentComplete).toInt,
					finished = false,
					succeeded = false,
					reportReceived = partitionedReports("reportReceived").size,
					reportRequested = partitionedReports("reportRequested").size,
					fileSubmitted = partitionedReports("fileSubmitted").size,
					awaitingSubmission = partitionedReports("awaitingSubmission").size
				)
			}
		}
	}
}

@Service("turnitinLtiQueueService")
class AutowiringEventServiceImpl
	extends AbstractTurnitinLtiQueueService
	with AutowriringTurnitinLtiQueueDaoComponent
	with AutowiringOriginalityReportServiceComponent
	with AutowiringFeaturesComponent

trait TurnitinLtiQueueServiceComponent {
	def turnitinLtiQueueService: TurnitinLtiQueueService
}

trait AutowiringTurnitinLtiQueueServiceComponent extends TurnitinLtiQueueServiceComponent {
	var turnitinLtiQueueService: TurnitinLtiQueueService = Wire[TurnitinLtiQueueService]
}

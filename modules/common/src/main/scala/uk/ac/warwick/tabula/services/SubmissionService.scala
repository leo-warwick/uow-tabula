package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.{AutowiringOriginalityReportDaoComponent, SubmissionDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire

trait SubmissionService {
	def saveSubmission(submission: Submission)
	def getSubmissionByUniId(assignment: Assignment, uniId: String): Option[Submission]
	def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission]
	def getSubmission(id: String): Option[Submission]

	def delete(submission: Submission): Unit
}

trait OriginalityReportService {
	def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport]
	def deleteOriginalityReport(attachment: FileAttachment): Unit
	def saveOriginalityReport(attachment: FileAttachment): Unit
}

@Service(value = "submissionService")
class SubmissionServiceImpl extends SubmissionService with Logging {
	self: SubmissionDaoComponent =>

	def saveSubmission(submission: Submission) = submissionDao.saveSubmission(submission)

	def getSubmissionByUniId(assignment: Assignment, uniId: String) = submissionDao.getSubmissionByUniId(assignment, uniId)

	def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission] = submissionDao.getSubmissionsByAssignment(assignment)

	def getSubmission(id: String) = submissionDao.getSubmission(id)

	def delete(submission: Submission) = submissionDao.delete(submission)
}

trait SubmissionServiceComponent {
	def submissionService: SubmissionService
}

trait AutowiringSubmissionServiceComponent extends SubmissionServiceComponent {
	var submissionService = Wire[SubmissionService]
}

@Service(value = "originalityReportService")
class OriginalityReportServiceImpl extends OriginalityReportService with Logging with AutowiringOriginalityReportDaoComponent {

	def deleteOriginalityReport(attachment: FileAttachment) = originalityReportDao.deleteOriginalityReport(attachment)

	def saveOriginalityReport(attachment: FileAttachment) = originalityReportDao.saveOriginalityReport(attachment)

	def getOriginalityReportByFileId(fileId: String) = originalityReportDao.getOriginalityReportByFileId(fileId)
}

trait OriginalityReportServiceComponent {
	def originalityReportService: OriginalityReportService
}

trait AutowiringOriginalityReportServiceComponent extends OriginalityReportServiceComponent {
	var originalityReportService = Wire[OriginalityReportService]
}
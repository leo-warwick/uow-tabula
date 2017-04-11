package uk.ac.warwick.tabula.data

import org.joda.time.DateTime
import org.junit.{After, Before}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport, Submission}
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}

import scala.collection.JavaConverters._

class TurnitinLtiQueueDaoTest extends PersistenceTestBase {

	val turnitinLtiQueueDao = new TurnitinLtiQueueDaoImpl

	@Before def setup(): Unit = {
		turnitinLtiQueueDao.sessionFactory = sessionFactory
	}

	@After def tidyUp(): Unit = transactional { tx =>
		session.createCriteria(classOf[Assignment]).list().asInstanceOf[JList[Assignment]].asScala.foreach(session.delete)
		session.createCriteria(classOf[Submission]).list().asInstanceOf[JList[Submission]].asScala.foreach(session.delete)
		session.createCriteria(classOf[SavedFormValue]).list().asInstanceOf[JList[SavedFormValue]].asScala.foreach(session.delete)
		session.createCriteria(classOf[FileAttachment]).list().asInstanceOf[JList[FileAttachment]].asScala.foreach(session.delete)
		session.createCriteria(classOf[OriginalityReport]).list().asInstanceOf[JList[OriginalityReport]].asScala.foreach(session.delete)
	}

	@Test
	def findAssignmentToProcessSuccess(): Unit = transactional { tx => {
		val assignmentToProcess = Fixtures.assignment("test")
		assignmentToProcess.submitToTurnitin = true
		assignmentToProcess.lastSubmittedToTurnitin = DateTime.now.minusMinutes(20)
		assignmentToProcess.turnitinId = null
		assignmentToProcess.submitToTurnitinRetries = 0
		session.save(assignmentToProcess)

		val assignmentToProcessLater = Fixtures.assignment("test")
		assignmentToProcessLater.submitToTurnitin = true
		assignmentToProcessLater.lastSubmittedToTurnitin = DateTime.now.minusMinutes(10)
		assignmentToProcessLater.turnitinId = null
		assignmentToProcessLater.submitToTurnitinRetries = 0
		session.save(assignmentToProcessLater)

		turnitinLtiQueueDao.findAssignmentToProcess.get should be (assignmentToProcess)
	}}

	@Test
	def findAssignmentToProcessNoneFound(): Unit = transactional { tx => {
		val assignmentDontSubmit = Fixtures.assignment("test")
		assignmentDontSubmit.submitToTurnitin = false
		session.save(assignmentDontSubmit)

		val assignmentTooSoon = Fixtures.assignment("test")
		assignmentTooSoon.submitToTurnitin = true
		assignmentTooSoon.lastSubmittedToTurnitin = DateTime.now
		session.save(assignmentTooSoon)

		val assignmentAlreadyDone = Fixtures.assignment("test")
		assignmentAlreadyDone.submitToTurnitin = true
		assignmentAlreadyDone.lastSubmittedToTurnitin = DateTime.now.minusHours(1)
		assignmentAlreadyDone.turnitinId = "1234"
		session.save(assignmentAlreadyDone)

		val assignmentTooManyRetries = Fixtures.assignment("test")
		assignmentTooManyRetries.submitToTurnitin = true
		assignmentTooManyRetries.lastSubmittedToTurnitin = DateTime.now.minusHours(1)
		assignmentTooManyRetries.turnitinId = null
		assignmentTooManyRetries.submitToTurnitinRetries = TurnitinLtiService.SubmitAssignmentMaxRetries
		session.save(assignmentTooManyRetries)

		turnitinLtiQueueDao.findAssignmentToProcess.isEmpty should be {true}
	}}

	private def fullCompleteReport(assignment: Assignment): OriginalityReport = {
		val report = new OriginalityReport
		report.reportReceived = true
		report.submitToTurnitinRetries = 0
		report.reportRequestRetries = 0
		val attachment = new FileAttachment
		val submissionValue = new SavedFormValue
		val submission = Fixtures.submission()
		report.attachment = attachment
		attachment.submissionValue = submissionValue
		submissionValue.submission = submission
		submission.assignment = assignment
		session.save(attachment)
		session.save(submission)
		session.save(submissionValue)
		session.save(attachment)
		report
	}

	private def validReportForSubmission(assignment: Assignment): OriginalityReport = {
		val report = fullCompleteReport(assignment)
		report.reportReceived = false
		report.turnitinId = null
		report.lastSubmittedToTurnitin = DateTime.now.minusMinutes(20)
		report.submitToTurnitinRetries = 0
		report
	}

	@Test
	def findReportToProcessForSubmissionSuccess(): Unit = transactional { tx => {
		val assignment = Fixtures.assignment("done")
		assignment.turnitinId = "1234"
		assignment.submitToTurnitin = true
		session.save(assignment)

		val reportToProcess = validReportForSubmission(assignment)
		session.save(reportToProcess)

		val reportToProcessLater = validReportForSubmission(assignment)
		reportToProcessLater.lastSubmittedToTurnitin = DateTime.now.minusMinutes(10)
		session.save(reportToProcessLater)

		turnitinLtiQueueDao.findReportToProcessForSubmission.get should be (reportToProcess)
	}}

	@Test
	def findReportToProcessForSubmissionNoneFound(): Unit = transactional { tx => {
		val assignment = Fixtures.assignment("done")
		assignment.turnitinId = "1234"
		assignment.submitToTurnitin = true
		session.save(assignment)

		val reportAlreadyDone = validReportForSubmission(assignment)
		reportAlreadyDone.turnitinId = "1234"
		session.save(reportAlreadyDone)

		val reportTooSoon = validReportForSubmission(assignment)
		reportTooSoon.lastSubmittedToTurnitin = DateTime.now
		session.save(reportTooSoon)

		val reportTooManyRetries = validReportForSubmission(assignment)
		reportTooManyRetries.submitToTurnitinRetries = TurnitinLtiService.SubmitAttachmentMaxRetries
		session.save(reportTooManyRetries)

		turnitinLtiQueueDao.findReportToProcessForSubmission.isEmpty should be {true}
	}}

	private def validReportForReport(assignment: Assignment): OriginalityReport = {
		val report = fullCompleteReport(assignment)
		report.turnitinId = "1234"
		report.fileRequested = DateTime.now
		report.reportReceived = false
		report.lastReportRequest = DateTime.now.minusMinutes(20)
		report.reportRequestRetries = 0
		report
	}

	@Test
	def findReportToProcessForReportSuccess(): Unit = transactional { tx => {
		val assignment = Fixtures.assignment("done")
		assignment.turnitinId = "1234"
		assignment.submitToTurnitin = true
		session.save(assignment)

		val reportToProcess = validReportForReport(assignment)
		session.save(reportToProcess)

		val reportToProcessLater = validReportForReport(assignment)
		reportToProcessLater.lastReportRequest = DateTime.now.minusMinutes(10)
		session.save(reportToProcessLater)

		turnitinLtiQueueDao.findReportToProcessForReport(false).get should be (reportToProcess)
	}}

	@Test
	def findReportToProcessForReportNoneFound(): Unit = transactional { tx => {
		val assignment = Fixtures.assignment("done")
		assignment.turnitinId = "1234"
		assignment.submitToTurnitin = true
		session.save(assignment)

		val reportAlreadyDone = validReportForReport(assignment)
		reportAlreadyDone.reportReceived = true
		session.save(reportAlreadyDone.attachment)
		session.save(reportAlreadyDone)

		val reportTooSoon = validReportForReport(assignment)
		reportTooSoon.lastReportRequest = DateTime.now
		session.save(reportTooSoon.attachment)
		session.save(reportTooSoon)

		val reportTooManyRetries = validReportForReport(assignment)
		reportTooManyRetries.reportRequestRetries = TurnitinLtiService.ReportRequestMaxRetries
		session.save(reportTooManyRetries.attachment)
		session.save(reportTooManyRetries)

		val reportNotSubmitted = validReportForReport(assignment)
		reportNotSubmitted.turnitinId = null
		session.save(reportNotSubmitted.attachment)
		session.save(reportNotSubmitted)

		turnitinLtiQueueDao.findReportToProcessForReport(false).isEmpty should be {true}
	}}

	@Test
	def listCompletedAssignments(): Unit = transactional { tx =>
  	val completeAssignment = Fixtures.assignment("done")
		completeAssignment.turnitinId = "1234"
		completeAssignment.submitToTurnitin = true
		session.save(completeAssignment)

		val completeReportReceieved = fullCompleteReport(completeAssignment)
		completeReportReceieved.reportReceived = true
		session.save(completeReportReceieved)
		val completeReportMaxSubmitRetry = fullCompleteReport(completeAssignment)
		completeReportMaxSubmitRetry.submitToTurnitinRetries = TurnitinLtiService.SubmitAttachmentMaxRetries
		session.save(completeReportMaxSubmitRetry)
		val completeReportMaxReportRetry = fullCompleteReport(completeAssignment)
		completeReportMaxReportRetry.reportRequestRetries = TurnitinLtiService.ReportRequestMaxRetries
		session.save(completeReportMaxReportRetry)

		val incompleteAssignment = Fixtures.assignment("not-yet")
		incompleteAssignment.turnitinId = "1234"
		incompleteAssignment.submitToTurnitin = true
		session.save(incompleteAssignment)

		val incompleteReport = fullCompleteReport(incompleteAssignment)
		incompleteReport.reportReceived = false
		incompleteReport.submitToTurnitinRetries = 0
		incompleteReport.reportRequestRetries = 0
		session.save(incompleteReport)

		val result = turnitinLtiQueueDao.listCompletedAssignments
		result.size should be (1)
		result.head should be (completeAssignment)
	}

}

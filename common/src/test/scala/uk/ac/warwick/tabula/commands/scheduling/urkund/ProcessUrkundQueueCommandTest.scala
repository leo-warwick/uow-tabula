package uk.ac.warwick.tabula.commands.scheduling.urkund

import org.apache.http.conn.ConnectTimeoutException
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport, Submission}
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.services.{OriginalityReportService, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.services.urkund._
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.util.{Failure, Success}

class ProcessUrkundQueueCommandTest extends TestBase with Mockito {

	val now: DateTime = DateTime.now

	trait Fixture {
		val report = new OriginalityReport
		val attahment = new FileAttachment
		val submissionValue = new SavedFormValue
		val submission = new Submission
		val assignment = new Assignment
		report.attachment = attahment
		attahment.submissionValue = submissionValue
		submissionValue.submission = submission
		submission.assignment = assignment

		val command = new ProcessUrkundQueueCommandInternal
			with UrkundServiceComponent with OriginalityReportServiceComponent {
			override val urkundService: UrkundService = smartMock[UrkundService]
			override val originalityReportService: OriginalityReportService = smartMock[OriginalityReportService]
		}
	}

	@Test
	def noMatchingReports(): Unit = new Fixture {
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns None
		command.applyInternal() should be (None)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(0)).retrieveReport(any[OriginalityReport])
	}

	@Test
	def submitReportSuccess(): Unit = withFakeTime(now) { new Fixture {
		report.nextSubmitAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns Some(report)
		command.urkundService.submit(report) returns Success(UrkundSuccessResponse(202, "The response", None, null, null, null, null, null, null, null))
		command.applyInternal() should be (None)
		report.nextSubmitAttempt should be (null)
		report.nextResponseAttempt should be (now.plusMinutes(UrkundService.reportTimeoutInMinutes))
		verify(command.urkundService, times(1)).submit(report)
		verify(command.urkundService, times(0)).retrieveReport(any[OriginalityReport])
		verify(command.urkundService, times(0)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def submitReportClientError(): Unit = withFakeTime(now) { new Fixture {
		report.nextSubmitAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns Some(report)
		command.urkundService.submit(report) returns Success(UrkundErrorResponse(0, "The response"))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (Option(assignment))
		report.nextSubmitAttempt should be (null)
		report.nextResponseAttempt should be (null)
		verify(command.urkundService, times(1)).submit(report)
		verify(command.urkundService, times(0)).retrieveReport(any[OriginalityReport])
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def submitReportServerError(): Unit = withFakeTime(now) { new Fixture {
		report.nextSubmitAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns Some(report)
		command.urkundService.submit(report) returns Failure(new ConnectTimeoutException)
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (None)
		report.nextSubmitAttempt should be (now.plusMinutes(UrkundService.serverErrorTimeoutInMinutes))
		report.nextResponseAttempt should be (null)
		verify(command.urkundService, times(1)).submit(report)
		verify(command.urkundService, times(0)).retrieveReport(any[OriginalityReport])
		verify(command.urkundService, times(0)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessAnalysed(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Analyzed,
			document = Option(UrkundDocument(3456, now, "", "")),
			report = Option(UrkundReport(4567, "http://www.foo.com", 0.65432.toFloat, 5, 6, Seq()))
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (Some(assignment))
		report.nextResponseAttempt should be (null)
		report.reportUrl should be ("http://www.foo.com")
		report.significance.toString should be (0.65432.toFloat.toString)
		report.matchCount should be (5)
		report.sourceCount should be (6)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessRejected(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Rejected,
			document = None,
			report = None
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (Some(assignment))
		report.nextResponseAttempt should be (null)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessError(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Error,
			document = None,
			report = None
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (Some(assignment))
		report.nextResponseAttempt should be (null)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessAccepted(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Accepted,
			document = None,
			report = None
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (None)
		report.nextResponseAttempt should be (now.plusMinutes(UrkundService.reportTimeoutInMinutes))
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(0)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessSubmitted(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Submitted,
			document = None,
			report = None
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (None)
		report.nextResponseAttempt should be (now.plusMinutes(UrkundService.reportTimeoutInMinutes))
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(0)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportClientError(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundErrorResponse(statusCode = 400, response = "The response"))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (Some(assignment))
		report.nextResponseAttempt should be (null)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportServerError(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Failure(new ConnectTimeoutException)
		command.urkundService.listOriginalityReports(assignment) returns Seq(report)
		command.applyInternal() should be (None)
		report.nextResponseAttempt should be (now.plusMinutes(UrkundService.serverErrorTimeoutInMinutes))
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(0)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

	@Test
	def retrieveReportSuccessAnalysedNotCompleteAssignment(): Unit = withFakeTime(now) { new Fixture {
		report.nextResponseAttempt = DateTime.now.minusMinutes(1)
		command.urkundService.findReportToSubmit returns None
		command.urkundService.findReportToRetreive returns Some(report)
		command.urkundService.retrieveReport(report) returns Success(UrkundSuccessResponse(
			statusCode = 200,
			response = "The response",
			submissionId = Some(1234),
			externalId = "2345",
			timestamp = now,
			filename = "test.doc",
			contentType = "application/octet-stream",
			status = UrkundSubmissionStatus.Analyzed,
			document = Option(UrkundDocument(3456, now, "", "")),
			report = Option(UrkundReport(4567, "http://www.foo.com", 0.65432.toFloat, 5, 6, Seq()))
		))
		command.urkundService.listOriginalityReports(assignment) returns Seq(report, new OriginalityReport {
			nextResponseAttempt = now.plusMinutes(5)
		})
		command.applyInternal() should be (None)
		report.nextResponseAttempt should be (null)
		report.reportUrl should be ("http://www.foo.com")
		report.significance.toString should be (0.65432.toFloat.toString)
		report.matchCount should be (5)
		report.sourceCount should be (6)
		verify(command.urkundService, times(0)).submit(any[OriginalityReport])
		verify(command.urkundService, times(1)).retrieveReport(report)
		verify(command.urkundService, times(1)).listOriginalityReports(assignment)
		verify(command.originalityReportService, times(1)).saveOrUpdate(report)
	}}

}

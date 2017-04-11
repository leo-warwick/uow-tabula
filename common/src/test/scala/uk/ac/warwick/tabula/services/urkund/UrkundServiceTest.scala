package uk.ac.warwick.tabula.services.urkund

import java.io.ByteArrayInputStream

import dispatch.classic.Http
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.message.BasicHttpResponse
import org.apache.http.{HttpHost, HttpRequest, HttpVersion}
import org.joda.time.{DateTime, DateTimeZone}
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{UrkundDao, UrkundDaoComponent}
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.{Fixtures, JsonObjectMapperFactory, Mockito, TestBase}

import scala.util.{Failure, Success}

class UrkundServiceTest extends TestBase with Mockito {

	trait Fixture {
		val mockObjectStorageService: ObjectStorageService = smartMock[ObjectStorageService]

		val service = new AbstractUrkundService with UrkundDaoComponent {
			override val http: Http = new Http {
				override def make_client: HttpClient = smartMock[HttpClient]
			}

			override val urkundDao: UrkundDao = smartMock[UrkundDao]
		}
		service.username = "username"
		service.password = "password"
		service.analysisPrefix = "dev"
		service.objectMapper = JsonObjectMapperFactory.instance

		val report = new OriginalityReport
		report.id = "2345"
		val attachment = new FileAttachment
		attachment.name = "submission.docx"
		attachment.objectStorageService = mockObjectStorageService
		val string = "Doe, a deer, a female deer"
		val bytes: Array[Byte] = string.getBytes("UTF-8")
		attachment.id = "1234"
		mockObjectStorageService.fetch(attachment.id) returns Some(new ByteArrayInputStream(bytes))

		val submissionValue = new SavedFormValue
		val submission: Submission = Fixtures.submission("1234")
		report.attachment = attachment
		attachment.submissionValue = submissionValue
		submissionValue.submission = submission
		val assignment: Assignment = Fixtures.assignment("test")
		assignment.id = "2345"
		submission.assignment = assignment
		val module: Module = Fixtures.module("its01")
		assignment.module = module
	}

	@Test
	def submitSuccessNewReceiver(): Unit = new Fixture {
		// Mock the receiver check
		val findReceiverHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 404, "Not Found")

		// Mock the receiver created response
		val createReceiverHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 201, "Created")
		createReceiverHttpResponse.setEntity(new StringEntity(
			"""
				{
					"Id": 123425,
					"UnitId": 5,
					"FullName": "John Doe",
					"EmailAddress": "john.doe@school.com",
					"AnalysisAddress": "2345.its01.tabula.dev@analysis.urkund.com",
					"Organization": null,
					"SubOrganization": null
				}
			""", ContentType.APPLICATION_JSON))

		// Mock the request
		val submitHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 202, "OK")
		submitHttpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Submitted",
						"Message": ""
					},
					"Document": null,
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		// Handle each request
		service.http.client.execute(any[HttpHost], any[HttpRequest]) answers { args =>
			args.asInstanceOf[Array[_]](1).asInstanceOf[HttpRequestBase].getURI.getPath match {
				case uri if uri.contains(UrkundService.receiversBaseUrl.replace(UrkundService.baseUrl, "") + "/2345.its01.tabula.dev@analysis.urkund.com") => findReceiverHttpResponse
				case uri if uri.contains(UrkundService.receiversBaseUrl.replace(UrkundService.baseUrl, "")) => createReceiverHttpResponse
				case uri if uri.contains(UrkundService.documentBaseUrl.replace(UrkundService.baseUrl, "")) => submitHttpResponse
				case uri => throw new IllegalArgumentException(s"Unexpected request URI: $uri")
			}
		}

		val response: UrkundSuccessResponse = service.submit(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail(s"Not a success response")
		}
		response.statusCode should be (202)
		response.submissionId should be (Some(3456))
		response.externalId should be (report.id)
		response.timestamp.getMillis should be (new DateTime(2016, 7, 5, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.filename should be (attachment.name)
		response.contentType should be ("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
		response.status should be (UrkundSubmissionStatus.Submitted)
		response.document should be (None)
		response.report should be (None)
	}

	@Test
	def submitSuccessExistingReceiver(): Unit = new Fixture {
		// Mock the receiver check
		val findReceiverHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		findReceiverHttpResponse.setEntity(new StringEntity(
			"""
				{
					"Id": 123425,
					"UnitId": 5,
					"FullName": "John Doe",
					"EmailAddress": "john.doe@school.com",
					"AnalysisAddress": "2345.its01.tabula.dev@analysis.urkund.com",
					"Organization": null,
					"SubOrganization": null
				}
			""", ContentType.APPLICATION_JSON))

		// Mock the request
		val submitHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 202, "OK")
		submitHttpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Submitted",
						"Message": ""
					},
					"Document": null,
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		// Handle each request
		service.http.client.execute(any[HttpHost], any[HttpRequest]) answers { args =>
			args.asInstanceOf[Array[_]](1).asInstanceOf[HttpRequestBase].getURI.getPath match {
				case uri if uri.contains(UrkundService.receiversBaseUrl.replace(UrkundService.baseUrl, "") + "/2345.its01.tabula.dev@analysis.urkund.com") => findReceiverHttpResponse
				case uri if uri.contains(UrkundService.documentBaseUrl.replace(UrkundService.baseUrl, "")) => submitHttpResponse
				case uri => throw new IllegalArgumentException(s"Unexpected request URI: $uri")
			}
		}

		val response: UrkundSuccessResponse = service.submit(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}
		response.statusCode should be (202)
		response.submissionId should be (Some(3456))
		response.externalId should be (report.id)
		response.timestamp.getMillis should be (new DateTime(2016, 7, 5, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.filename should be (attachment.name)
		response.contentType should be ("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
		response.status should be (UrkundSubmissionStatus.Submitted)
		response.document should be (None)
		response.report should be (None)
	}

	@Test
	def submitClientError(): Unit = new Fixture {
		// Mock the receiver check
		val findReceiverHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		findReceiverHttpResponse.setEntity(new StringEntity(
			"""
				{
					"Id": 123425,
					"UnitId": 5,
					"FullName": "John Doe",
					"EmailAddress": "john.doe@school.com",
					"AnalysisAddress": "2345.its01.tabula.dev@analysis.urkund.com",
					"Organization": null,
					"SubOrganization": null
				}
			""", ContentType.APPLICATION_JSON))

		// Mock the request
		val submitHttpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, "Bad Request")

		// Handle each request
		service.http.client.execute(any[HttpHost], any[HttpRequest]) answers { args =>
			args.asInstanceOf[Array[_]](1).asInstanceOf[HttpRequestBase].getURI.getPath match {
				case uri if uri.contains(UrkundService.receiversBaseUrl.replace(UrkundService.baseUrl, "") + "/2345.its01.tabula.dev@analysis.urkund.com") => findReceiverHttpResponse
				case uri if uri.contains(UrkundService.documentBaseUrl.replace(UrkundService.baseUrl, "")) => submitHttpResponse
				case uri => throw new IllegalArgumentException(s"Unexpected request URI: $uri")
			}
		}

		val response: UrkundErrorResponse = service.submit(report) match {
			case success: Success[UrkundErrorResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}
		response.statusCode should be (400)
	}

	@Test
	def submitServerError(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 500, "Oh noes!")

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		service.submit(report) match {
			case failure: Failure[_] => null
			case _ => fail("Not a failure response")
		}
	}

	@Test
	def reportSubmitted(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		httpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Submitted",
						"Message": ""
					},
					"Document": null,
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		val response: UrkundSuccessResponse = service.retrieveReport(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}
		response.statusCode should be (200)
		response.submissionId should be (Some(3456))
		response.externalId should be (report.id)
		response.timestamp.getMillis should be (new DateTime(2016, 7, 5, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.filename should be (attachment.name)
		response.contentType should be ("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
		response.status should be (UrkundSubmissionStatus.Submitted)
		response.document should be (None)
		response.report should be (None)
	}

	@Test
	def reportRejected(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		httpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Rejected",
						"Message": ""
					},
					"Document": null,
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		val response: UrkundSuccessResponse = service.retrieveReport(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}

		response.status should be (UrkundSubmissionStatus.Rejected)
		response.document should be (None)
		response.report should be (None)
	}

	@Test
	def reportAccepted(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		httpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Accepted",
						"Message": ""
					},
					"Document": {
		 				"Id": 4567,
						"Date": "2016-07-06T12:24:36Z",
						"DownloadUrl": "http://www.test.com",
						"OptOutInfo": {
							"Url": "http://www.foo.com",
			 				"Message": "Some message"
						}
					},
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		val response: UrkundSuccessResponse = service.retrieveReport(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}

		response.status should be (UrkundSubmissionStatus.Accepted)
		response.document.nonEmpty should be (true)
		response.document.get.id should be (4567)
		response.document.get.acceptedDate.getMillis should be (new DateTime(2016, 7, 6, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.document.get.downloadUrl should be ("http://www.test.com")
		response.document.get.optOutUrl should be ("http://www.foo.com")
		response.report should be (None)
	}

	@Test
	def reportError(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		httpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Error",
						"Message": ""
					},
					"Document": {
		 				"Id": 4567,
						"Date": "2016-07-06T12:24:36Z",
						"DownloadUrl": "http://www.test.com",
						"OptOutInfo": {
							"Url": "http://www.foo.com",
			 				"Message": "Some message"
						}
					},
					"Report": null,
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		val response: UrkundSuccessResponse = service.retrieveReport(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case _ => fail("Not a success response")
		}

		response.status should be (UrkundSubmissionStatus.Error)
		response.document.nonEmpty should be (true)
		response.document.get.id should be (4567)
		response.document.get.acceptedDate.getMillis should be (new DateTime(2016, 7, 6, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.document.get.downloadUrl should be ("http://www.test.com")
		response.document.get.optOutUrl should be ("http://www.foo.com")
		response.report should be (None)
	}

	@Test
	def reportAnalyzed(): Unit = new Fixture {
		// Mock the request
		val httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		httpResponse.setEntity(new StringEntity(
			"""
				{
					"SubmissionId": 3456,
					"ExternalId": "2345",
					"Timestamp": "2016-07-05T12:24:36Z",
					"Filename": "submission.docx",
					"MimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					"Status": {
						"State": "Analyzed",
						"Message": ""
					},
					"Document": {
		 				"Id": 4567,
						"Date": "2016-07-06T12:24:36Z",
						"DownloadUrl": "http://www.test.com",
						"OptOutInfo": {
							"Url": "http://www.foo.com",
			 				"Message": "Some message"
						}
					},
					"Report": {
		 				"Id": 5678,
			 			"ReportUrl": "http://www.bar.com",
			 			"Significance": 0.7894349,
			 			"MatchCount": 5,
			 			"SourceCount": 6,
			 			"Warnings": [
			 				{
								"WarningType": "Warning A",
								"Excerpt": "test excerpt A",
								"Message": "test message A"
			 				},
							{
								"WarningType": "Warning B",
								"Excerpt": "test excerpt B",
								"Message": "test message B"
							}
						]
					},
					"Subject": null,
					"Message": null,
					"Anonymous": null
				}
			""", ContentType.APPLICATION_JSON))

		service.http.client.execute(any[HttpHost], any[HttpRequest]) returns httpResponse

		val response: UrkundSuccessResponse = service.retrieveReport(report) match {
			case success: Success[UrkundSuccessResponse] @unchecked => success.value
			case failure => fail("Not a success response")
		}

		response.status should be (UrkundSubmissionStatus.Analyzed)
		response.document.nonEmpty should be (true)
		response.document.get.id should be (4567)
		response.document.get.acceptedDate.getMillis should be (new DateTime(2016, 7, 6, 12, 24, 36, DateTimeZone.UTC).getMillis)
		response.document.get.downloadUrl should be ("http://www.test.com")
		response.document.get.optOutUrl should be ("http://www.foo.com")
		response.report.nonEmpty should be (true)
		response.report.get.id should be (5678)
		response.report.get.reportUrl should be ("http://www.bar.com")
		response.report.get.significance.toString should be ("0.7894349")
		response.report.get.matchCount should be (5)
		response.report.get.sourceCount should be (6)
		response.report.get.warnings.size should be (2)
	}

	val now: DateTime = DateTime.now

	@Test
	def nextSubmitAttempt(): Unit = withFakeTime(now){
		val report = new OriginalityReport

		// If first submission attempt fails, retry in 15 mins (currently 0 mins after first attempt)
		report.submitAttempts = 1
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusMinutes(15).getMillis)

		// If second submission attempt fails, retry in 30 mins (currently 15 mins after first attempt)
		report.submitAttempts = 2
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusMinutes(30).getMillis)

		// If third submission attempt fails, retry in 60 mins (currently 45 mins after first attempt)
		report.submitAttempts = 3
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusMinutes(60).getMillis)

		// If fourth submission attempt fails, retry in 2 hours (currently 105 mins after first attempt)
		report.submitAttempts = 4
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusHours(2).getMillis)

		// If fifth submission attempt fails, retry in 4 hours (currently 3.75 hrs after first attempt)
		report.submitAttempts = 5
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusHours(4).getMillis)

		// If sixth submission attempt fails, retry in 8 hours (currently 7.75 hrs after first attempt)
		report.submitAttempts = 6
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusHours(8).getMillis)

		// If sixth submission attempt fails, retry in 16 hours (currently 15.75 hrs after first attempt)
		report.submitAttempts = 7
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt.getMillis should be (now.plusHours(16).getMillis)

		// If the eighth submission attempt fails it's been more than 24 hours so stop trying
		report.submitAttempts = 8
		UrkundService.setNextSubmitAttempt(report)
		report.nextSubmitAttempt should be (null)
	}

	@Test
	def nextResponseAttempt(): Unit = withFakeTime(now){
		val report = new OriginalityReport

		// After first attempt wait 30 mins (currently 30 mins after submission)
		report.responseAttempts = 1
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusMinutes(30).getMillis)

		// After second attempt wait 60 mins (currently 60 mins after submission)
		report.responseAttempts = 2
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusMinutes(60).getMillis)

		// After third attempt wait 2 hours (currently 2 hrs after submission)
		report.responseAttempts = 3
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(2).getMillis)

		// After fourth attempt wait 4 hours (currently 4 hrs after submission)
		report.responseAttempts = 4
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(4).getMillis)

		// After fifth attempt wait 8 hours (currently 8 hrs after submission)
		report.responseAttempts = 5
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// After sixth attempt wait 8 hours (currently 16 hrs after submission)
		report.responseAttempts = 6
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// After seventh attempt wait 8 hours (currently 24 hrs after submission)
		report.responseAttempts = 7
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// After eighth attempt wait 8 hours (currently 32 hrs after submission)
		report.responseAttempts = 8
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// After ninth attempt wait 8 hours (currently 40 hrs after submission)
		report.responseAttempts = 9
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// If tenth attempt fails it's been 48 hours so stop trying
		report.responseAttempts = 10
		UrkundService.setNextResponseAttempt(report)
		report.nextResponseAttempt should be (null)
	}

	@Test
	def nextResponseAttemptOnError(): Unit = withFakeTime(now){
		val report = new OriginalityReport

		// If first submission attempt fails, retry in 15 mins (currently 0 mins after first attempt)
		report.responseAttempts = 1
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusMinutes(15).getMillis)

		// If second submission attempt fails, retry in 30 mins (currently 15 mins after first attempt)
		report.responseAttempts = 2
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusMinutes(30).getMillis)

		// If third submission attempt fails, retry in 60 mins (currently 45 mins after first attempt)
		report.responseAttempts = 3
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusMinutes(60).getMillis)

		// If fourth submission attempt fails, retry in 2 hours (currently 105 mins after first attempt)
		report.responseAttempts = 4
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(2).getMillis)

		// If fifth submission attempt fails, retry in 4 hours (currently 3.75 hrs after first attempt)
		report.responseAttempts = 5
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(4).getMillis)

		// If sixth submission attempt fails, retry in 8 hours (currently 7.75 hrs after first attempt)
		report.responseAttempts = 6
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(8).getMillis)

		// If sixth submission attempt fails, retry in 16 hours (currently 15.75 hrs after first attempt)
		report.responseAttempts = 7
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt.getMillis should be (now.plusHours(16).getMillis)

		// If the eighth submission attempt fails it's been more than 24 hours so stop trying
		report.responseAttempts = 8
		UrkundService.setNextResponseAttemptOnError(report)
		report.nextResponseAttempt should be (null)
	}

}

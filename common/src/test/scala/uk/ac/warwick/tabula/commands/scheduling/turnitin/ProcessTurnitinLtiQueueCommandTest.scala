package uk.ac.warwick.tabula.commands.scheduling.turnitin

import org.joda.time.DateTime
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.scheduling.turnitin.ProcessTurnitinLtiQueueCommand.ProcessTurnitinLtiQueueCommandResult
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model.notifications.coursework.{TurnitinJobErrorNotification, TurnitinJobSuccessNotification}
import uk.ac.warwick.tabula.services.turnitinlti._
import uk.ac.warwick.tabula.services.{OriginalityReportServiceComponent, _}
import uk.ac.warwick.userlookup.User

class ProcessTurnitinLtiQueueCommandTest extends TestBase with Mockito {

	val mockTurnitinLtiService: TurnitinLtiService = smartMock[TurnitinLtiService]
	val mockAssessmentService: AssessmentService = smartMock[AssessmentService]
	val mockFileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]
	val mockOriginalityReportService: OriginalityReportService = smartMock[OriginalityReportService]
	val mockUserLookup = new MockUserLookup

	val now: DateTime = DateTime.now
	val department = new Department {
		code = "its"
		override lazy val owners: UserGroup = UserGroup.ofUsercodes
		owners.userLookup = mockUserLookup
	}
	val module = new Module {
		code = "aa100"
		override lazy val managers: UserGroup = UserGroup.ofUsercodes
		managers.userLookup = mockUserLookup
	}
	module.adminDepartment = department
	val user1 = new User("user1")
	user1.setFoundUser(true)
	val user2 = new User("user2")
	user2.setFoundUser(true)
	val user3 = new User("user3")
	user3.setFoundUser(true)
	module.managers.add(user2)
	department.owners.add(user3)
	mockUserLookup.registerUserObjects(user1, user2, user3)

	trait Fixture {
		val mockTurnitinLtiQueueService: TurnitinLtiQueueService = smartMock[TurnitinLtiQueueService]
		val cmd = new ProcessTurnitinLtiQueueCommandInternal with TurnitinLtiQueueServiceComponent
			with TurnitinLtiServiceComponent with AssessmentServiceComponent with FileAttachmentServiceComponent
			with OriginalityReportServiceComponent {
			override val topLevelUrl: String = ""
			override val turnitinLtiQueueService: TurnitinLtiQueueService = mockTurnitinLtiQueueService
			override val turnitinLtiService: TurnitinLtiService = mockTurnitinLtiService
			override val assessmentService: AssessmentService = mockAssessmentService
			override val fileAttachmentService: FileAttachmentService = mockFileAttachmentService
			override val originalityReportService: OriginalityReportService = mockOriginalityReportService
		}
	}

	private def deepOriginalityReport(): OriginalityReport = {
		val report = new OriginalityReport
		val attachment = new FileAttachment
		report.attachment = attachment
		val submissionValue = new SavedFormValue
		attachment.submissionValue = submissionValue
		val submission = Fixtures.submission()
		submissionValue.submission = submission
		val assignment = Fixtures.assignment("test")
		submission.assignment = assignment
		report
	}

	@Test
	def onlyRunFirst(): Unit = withFakeTime(now){
		// Assignment to run
		new Fixture {
			val assignment: Assignment = Fixtures.assignment("test")
			assignment.module = module
			assignment.turnitinLtiNotifyUsers = Seq(user1)
			assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns Option(assignment)
			mockTurnitinLtiService.submitAssignment(assignment, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(success = true)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			verify(mockTurnitinLtiQueueService, times(0)).findReportToProcessForSubmission
			verify(mockTurnitinLtiQueueService, times(0)).findReportToProcessForReport(false)
			assignment.lastSubmittedToTurnitin should be (now)
			verify(mockAssessmentService, times(1)).save(assignment)
			verify(mockTurnitinLtiQueueService, times(1)).listCompletedAssignments
			verify(mockTurnitinLtiQueueService, times(1)).listFailedAssignments
		}
		// Long awaited report to get
		new Fixture {
			val report: OriginalityReport = deepOriginalityReport()
			report.turnitinId = "1234"
			report.attachment.submissionValue.submission.assignment.turnitinLtiNotifyUsers = Seq(user1)
			report.attachment.submissionValue.submission.assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns Option(report)
			mockTurnitinLtiService.getSubmissionDetails(report.turnitinId, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(success = true, json = Option(""))
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastReportRequest should be (now)
			verify(mockTurnitinLtiQueueService, times(0)).findReportToProcessForSubmission
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
			verify(mockTurnitinLtiQueueService, times(1)).listCompletedAssignments
			verify(mockTurnitinLtiQueueService, times(1)).listFailedAssignments
		}
		// Submission to run
		new Fixture {
			val report: OriginalityReport = deepOriginalityReport()
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns Option(report)
			mockFileAttachmentService.getValidToken(report.attachment) returns None
			mockTurnitinLtiService.submitPaper(
				any[Assignment],
				any[String],
				any[String],
				any[String],
				any[FileAttachment],
				any[String],
				any[String]
			) returns new TurnitinLtiResponse(
				success = true,
				xml = Option(
					<response>
						<status>fullsuccess</status>
						<submission_data_extract>Some text</submission_data_extract>
						<lis_result_sourcedid>1234</lis_result_sourcedid>
						<message>Your file has been saved successfully.</message>
					</response>
				)
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			verify(mockTurnitinLtiQueueService, times(0)).findReportToProcessForReport(false)
			report.lastSubmittedToTurnitin should be (now)
			report.turnitinId should be ("1234")
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
			verify(mockTurnitinLtiQueueService, times(1)).listCompletedAssignments
			verify(mockTurnitinLtiQueueService, times(1)).listFailedAssignments
		}
		// Report to get
		new Fixture {
			val report: OriginalityReport = deepOriginalityReport()
			report.turnitinId = "1234"
			report.attachment.submissionValue.submission.assignment.turnitinLtiNotifyUsers = Seq(user1)
			report.attachment.submissionValue.submission.assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(false) returns Option(report)
			mockTurnitinLtiService.getSubmissionDetails(report.turnitinId, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(success = true, json = Option(""))
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastReportRequest should be (now)
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
			verify(mockTurnitinLtiQueueService, times(1)).listCompletedAssignments
			verify(mockTurnitinLtiQueueService, times(1)).listFailedAssignments
		}
	}

	@Test
	def assignmentWithNotifyUsers(): Unit = {
		new Fixture {
			val assignment: Assignment = Fixtures.assignment("test")
			assignment.module = module
			assignment.turnitinLtiNotifyUsers = Seq(user1)
			assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns Option(assignment)
			mockTurnitinLtiService.submitAssignment(assignment, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(success = true)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
		}
	}

	@Test
	def assignmentWithModuleManager(): Unit = {
		new Fixture {
			val assignment: Assignment = Fixtures.assignment("test")
			assignment.module = module
			assignment.turnitinLtiNotifyUsers = Seq()
			mockTurnitinLtiQueueService.findAssignmentToProcess returns Option(assignment)
			mockTurnitinLtiService.submitAssignment(assignment, new CurrentUser(user2, user2)) returns new TurnitinLtiResponse(success = true)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
		}
	}

	@Test
	def assignmentWithDepartmentOwner(): Unit = {
		new Fixture {
			val moduleWithoutManagers = new Module {
				code = "aa200"
				override lazy val managers: UserGroup = UserGroup.ofUsercodes
				managers.userLookup = mockUserLookup
			}
			moduleWithoutManagers.adminDepartment = department
			val assignment: Assignment = Fixtures.assignment("test")
			assignment.module = moduleWithoutManagers
			assignment.turnitinLtiNotifyUsers = Seq()
			mockTurnitinLtiQueueService.findAssignmentToProcess returns Option(assignment)
			mockTurnitinLtiService.submitAssignment(assignment, new CurrentUser(user3, user3)) returns new TurnitinLtiResponse(success = true)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
		}
	}

	@Test
	def assignmentWithFailure(): Unit = {
		new Fixture {
			val assignment: Assignment = Fixtures.assignment("test")
			assignment.module = module
			assignment.turnitinLtiNotifyUsers = Seq(user1)
			assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns Option(assignment)
			mockTurnitinLtiService.submitAssignment(assignment, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(success = false)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			assignment.submitToTurnitinRetries should be (1)
		}
	}

	trait ReportSetupFixture extends Fixture {
		val report: OriginalityReport = deepOriginalityReport()
		val attachment: FileAttachment = report.attachment
		attachment.id="1234"
		val submission: Submission = attachment.submissionValue.submission
		submission.id = "2345"
		submission.usercode = "3456"
		submission._universityId = "4567"
		val assignment: Assignment = submission.assignment
	}

	@Test
	def reportForSubmissionFailure(): Unit = withFakeTime(now) {
		new ReportSetupFixture {
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns Option(report)
			mockFileAttachmentService.getValidToken(report.attachment) returns None
			mockTurnitinLtiService.submitPaper(
				assignment,
				s"/turnitin/submission/${submission.id}/attachment/${attachment.id}?token=null",
				submission.usercode,
				s"${submission.usercode}@TurnitinLti.warwick.ac.uk",
				attachment,
				submission._universityId,
				"Student"
			) returns new TurnitinLtiResponse(
				success = false,
				statusMessage = Option("How bout no!")
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastSubmittedToTurnitin should be (now)
			report.submitToTurnitinRetries should be (1)
			report.lastTurnitinError should be ("How bout no!")
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
		}
	}

	@Test
	def reportForSubmissionSuccessNoToken(): Unit = withFakeTime(now) {
		new ReportSetupFixture {
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns Option(report)
			mockFileAttachmentService.getValidToken(report.attachment) returns None
			mockTurnitinLtiService.submitPaper(
				assignment,
				s"/turnitin/submission/${submission.id}/attachment/${attachment.id}?token=null",
				submission.usercode,
				s"${submission.usercode}@TurnitinLti.warwick.ac.uk",
				attachment,
				submission._universityId,
				"Student"
			) returns new TurnitinLtiResponse(
				success = true,
				xml = Option(
					<response>
						<status>fullsuccess</status>
						<submission_data_extract>Some text</submission_data_extract>
						<lis_result_sourcedid>5678</lis_result_sourcedid>
						<message>Your file has been saved successfully.</message>
					</response>
				)
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastSubmittedToTurnitin should be (now)
			report.submitToTurnitinRetries should be (0)
			report.turnitinId should be ("5678")
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
		}
	}

	@Test
	def reportForSubmissionSuccessExisitngToken(): Unit = withFakeTime(now) {
		new ReportSetupFixture {
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns Option(report)
			val token = new FileAttachmentToken
			token.id = "1234"
			mockFileAttachmentService.getValidToken(report.attachment) returns Option(token)
			mockTurnitinLtiService.submitPaper(
				assignment,
				s"/turnitin/submission/${submission.id}/attachment/${attachment.id}?token=${token.id}",
				submission.usercode,
				s"${submission.usercode}@TurnitinLti.warwick.ac.uk",
				attachment,
				submission._universityId,
				"Student"
			) returns new TurnitinLtiResponse(
				success = true,
				xml = Option(
					<response>
						<status>fullsuccess</status>
						<submission_data_extract>Some text</submission_data_extract>
						<lis_result_sourcedid>5678</lis_result_sourcedid>
						<message>Your file has been saved successfully.</message>
					</response>
				)
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastSubmittedToTurnitin should be (now)
			report.submitToTurnitinRetries should be (0)
			report.turnitinId should be ("5678")
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
		}
	}

	@Test
	def reportForReportFailure(): Unit = withFakeTime(now) {
		new ReportSetupFixture {
			report.turnitinId = "1234"
			assignment.turnitinLtiNotifyUsers = Seq(user1)
			assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns Option(report)
			mockTurnitinLtiService.getSubmissionDetails(report.turnitinId, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(
				success = false,
				statusMessage = Option("How bout no!")
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.lastReportRequest should be (now)
			report.reportRequestRetries should be (1)
			report.lastTurnitinError should be ("How bout no!")
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
		}
	}

	@Test
	def reportForReportSuccess(): Unit = withFakeTime(now) {
		new ReportSetupFixture {
			report.turnitinId = "1234"
			assignment.turnitinLtiNotifyUsers = Seq(user1)
			assignment.userLookup = mockUserLookup
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns Option(report)
			mockTurnitinLtiService.getSubmissionDetails(report.turnitinId, new CurrentUser(user1, user1)) returns new TurnitinLtiResponse(
				success = true,
				json = Option(
					"""
						{
							"outcome_originalityreport": {
			 					"breakdown": {
				 					"publications_score": 20,
									"internet_score": 30,
				 					"submitted_works_score": 40
			 					},
				 				"numeric": {
				 					"score": 50
				 				}
							}
						}
					"""
				)
			)
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq()
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq()
			cmd.applyInternal()
			report.similarity should be (Option(3))
			report.overlap should be (Option(50))
			report.publicationOverlap should be (Option(20))
			report.webOverlap should be (Option(30))
			report.studentOverlap should be (Option(40))
			report.reportReceived should be {true}
			verify(mockOriginalityReportService, times(1)).saveOrUpdate(report)
		}
	}

	@Test
	def finish(): Unit = {
		new Fixture {
			val completeAssignment: Assignment = Fixtures.assignment("test")
			completeAssignment.submitToTurnitin = true
			val failedAssignment: Assignment = Fixtures.assignment("test")
			failedAssignment.submitToTurnitin = true
			mockTurnitinLtiQueueService.findAssignmentToProcess returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(true) returns None
			mockTurnitinLtiQueueService.findReportToProcessForSubmission returns None
			mockTurnitinLtiQueueService.findReportToProcessForReport(false) returns None
			mockTurnitinLtiQueueService.listCompletedAssignments returns Seq(completeAssignment)
			mockTurnitinLtiQueueService.listFailedAssignments returns Seq(failedAssignment)
			cmd.applyInternal()
			completeAssignment.submitToTurnitin.booleanValue() should be {false}
			failedAssignment.submitToTurnitin.booleanValue() should be {false}
			verify(mockAssessmentService, times(1)).save(completeAssignment)
			verify(mockAssessmentService, times(1)).save(failedAssignment)
		}
	}

	trait NotificationFixture {
		val mockTurnitinLtiQueueService: TurnitinLtiQueueService = smartMock[TurnitinLtiQueueService]
		val cmd = new ProcessTurnitinLtiQueueNotification with TurnitinLtiQueueServiceComponent {
			override val turnitinLtiQueueService: TurnitinLtiQueueService = mockTurnitinLtiQueueService
		}
	}

	@Test
	def notifications(): Unit = {
		new NotificationFixture {
			val fullyCompletedAssignment: Assignment = Fixtures.assignment("done")
			fullyCompletedAssignment.turnitinLtiNotifyUsers = Seq(user1, user2)
			fullyCompletedAssignment.userLookup = mockUserLookup
			fullyCompletedAssignment.submissions.add(Fixtures.submission())
			val partiallyCompletedAssignment: Assignment = Fixtures.assignment("nearly")
			partiallyCompletedAssignment.turnitinLtiNotifyUsers = Seq(user1)
			partiallyCompletedAssignment.userLookup = mockUserLookup
			partiallyCompletedAssignment.submissions.add(Fixtures.submission())
			val failedAssignment: Assignment = Fixtures.assignment("nope")
			failedAssignment.turnitinLtiNotifyUsers = Seq(user1)
			failedAssignment.userLookup = mockUserLookup

			val fullyCompletedReport = new OriginalityReport
			fullyCompletedReport.reportReceived = true

			val failedReportOnSubmission = new OriginalityReport
			failedReportOnSubmission.reportReceived = false
			failedReportOnSubmission.submitToTurnitinRetries = TurnitinLtiService.SubmitAttachmentMaxRetries

			val failedReportOnReport = new OriginalityReport
			failedReportOnReport.reportReceived = false
			failedReportOnReport.reportRequestRetries = TurnitinLtiService.ReportRequestMaxRetries

			mockTurnitinLtiQueueService.listOriginalityReports(fullyCompletedAssignment) returns Seq(
				fullyCompletedReport
			)

			mockTurnitinLtiQueueService.listOriginalityReports(partiallyCompletedAssignment) returns Seq(
				fullyCompletedReport,
				failedReportOnSubmission,
				failedReportOnReport
			)

			val result: Seq[Notification[OriginalityReport, Assignment]] = cmd.emit(ProcessTurnitinLtiQueueCommandResult(
				completedAssignments = Seq(fullyCompletedAssignment, partiallyCompletedAssignment),
				failedAssignments = Seq(failedAssignment)
			))

			result.size should be (4) // Three assignments, 2 users for 1 assignment
			val notifications: Seq[NotificationWithTarget[OriginalityReport, Assignment]] = result.map(_.asInstanceOf[NotificationWithTarget[OriginalityReport, Assignment]])
			val (successNotifications, errorNotifications) = notifications.partition {
				case n: TurnitinJobSuccessNotification => true
				case n: TurnitinJobErrorNotification => false
			}
			successNotifications.size should be (3)
			successNotifications.count(_.agent == user1) should be (2)
			successNotifications.count(_.agent == user2) should be (1)
			successNotifications.find(_.target.entity == fullyCompletedAssignment).get.entities should be (Seq())
			successNotifications.find(_.target.entity == partiallyCompletedAssignment).get.entities should be (Seq(
				failedReportOnSubmission,
				failedReportOnReport
			))
			errorNotifications.size should be (1)
		}
	}

}
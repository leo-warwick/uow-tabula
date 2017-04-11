package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, DateTimeConstants}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{FileAttachmentService, FileAttachmentServiceComponent, MeetingRecordService, SecurityService, SecurityServiceComponent, _}
import uk.ac.warwick.tabula.{CurrentUser, Features, FeaturesComponent, _}

class EditMeetingRecordCommandTest extends TestBase with Mockito {

	val aprilFool: DateTime = dateTime(2013, DateTimeConstants.APRIL)
	val marchHare: DateTime = dateTime(2013, DateTimeConstants.MARCH)
	val thisCreator: StaffMember = Fixtures.staff("9876543")
	val student: StudentMember = Fixtures.student(universityId="1170836", userId="studentmember")
	val thisRelationship = ExternalStudentRelationship(
		"Professor A Tutor",
		StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee"),
		student,
		DateTime.now
	)

	@Test
	def creatorEditMeeting() = withUser(thisCreator.userId) {  withFakeTime(aprilFool) {
		val meeting = new MeetingRecord(thisCreator, thisRelationship)
		meeting.securityService = smartMock[SecurityService]

		val cmd = new EditMeetingRecordCommandInternal(meeting)
			with MeetingRecordCommandRequest
			with EditMeetingRecordCommandState
			with MeetingRecordServiceComponent
			with FeaturesComponent
			with AttendanceMonitoringMeetingRecordServiceComponent
			with FileAttachmentServiceComponent {
			override val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
			override val features: Features = Features.empty
			override val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
			override val fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]
		}

		cmd.title = "Updated title fools"
		cmd.features.meetingRecordApproval = true
		cmd.features.attendanceMonitoringMeetingPointType = false

		cmd.applyInternal()
		meeting.title should be ("Updated title fools")

		meeting.isPendingApproval should be (true)
		val studentCurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		meeting.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting.approvals.get(0)) returns true
		meeting.pendingApprovalBy(studentCurrentUser) should be (true)
	}}

	@Test
	def meetingRecordWorkflow() = withUser("cuslaj") { withFakeTime(aprilFool) {

		// Here is a story about the meeting record workflow ...
		// A student sees a meeting record with an inaccurate description. She tries to reject but forgets to add a comment
		val studentCurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		val meeting = new MeetingRecord(thisCreator, thisRelationship)
		meeting.securityService = smartMock[SecurityService]
		val origApproval = new MeetingRecordApproval
		origApproval.approver = thisRelationship.studentMember.get
		origApproval.creationDate = aprilFool
		origApproval.state = Pending
		origApproval.meetingRecord = meeting

		meeting.approvals.add(origApproval)

		var approvalCmd = new ApproveMeetingRecordCommand(meeting, studentCurrentUser) with ApproveMeetingRecordState with MeetingRecordDaoComponent
			with ApproveMeetingRecordValidation with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent with SecurityServiceComponent {
				val meetingRecordDao: MeetingRecordDao = smartMock[MeetingRecordDao]
				val features: Features = smartMock[Features]
				val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
				val securityService: SecurityService = smartMock[SecurityService]
			}

		approvalCmd.approved = false
		val errors = new BindException(approvalCmd, "command")
		approvalCmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("rejectionComments")
		errors.getFieldError.getCode should be ("meetingRecordApproval.rejectionComments.isEmpty")

		// Validation prompts them for a rejection comment. They rant about herons and reject.
		val heronRant = "There is no mention of herons in the meeting record. I distinctly remember LOADS of herons in my face."
		approvalCmd.rejectionComments = heronRant
		approvalCmd.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting.approvals.get(0)) returns true
		var approval = approvalCmd.applyInternal().approvals.get(0)
		approval.state should be (Rejected)
		approval.comments should be (heronRant)
		meeting.isRejected should be (true)
		meeting.pendingRevisionBy(new CurrentUser(thisCreator.asSsoUser, thisCreator.asSsoUser)) should be (true)

		// The tutor sees the rejection. They add a description about herons to placate the student.
		val editCmd = new EditMeetingRecordCommandInternal(meeting)
			with MeetingRecordCommandRequest
			with EditMeetingRecordCommandState
			with MeetingRecordServiceComponent
			with FeaturesComponent
			with AttendanceMonitoringMeetingRecordServiceComponent
			with FileAttachmentServiceComponent {
			override val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]
			override val features: Features = Features.empty
			override val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
			override val fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]
		}

		editCmd.description = "The meeting room was full of angry herons. It was truly harrowing."
		val meeting2 = editCmd.applyInternal()
		meeting2.isPendingApproval should be (true)
		meeting2.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting2.approvals.get(0)) returns true
		meeting2.pendingApprovalBy(studentCurrentUser) should be (true)

		// The student is now happy with the record and approves it
		approvalCmd = new ApproveMeetingRecordCommand(meeting, studentCurrentUser)
			with ApproveMeetingRecordState with MeetingRecordDaoComponent with SecurityServiceComponent
			with ApproveMeetingRecordValidation	with FeaturesComponent
			with AttendanceMonitoringMeetingRecordServiceComponent {
				val meetingRecordDao: MeetingRecordDao = smartMock[MeetingRecordDao]
				val features: Features = smartMock[Features]
				val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
				val securityService: SecurityService = smartMock[SecurityService]
			}
		approvalCmd.approved = true
		approvalCmd.rejectionComments = null
		approvalCmd.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting2.approvals.get(0)) returns true
		approval = approvalCmd.applyInternal().approvals.get(0)
		meeting2.isApproved should be (true)
		// Fin
	}}

}

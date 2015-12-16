package uk.ac.warwick.tabula.commands.profiles

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.MeetingRecordRejectedNotification
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermService, MonitoringPointMeetingRelationshipTermServiceComponent, SecurityService, SecurityServiceComponent}
import uk.ac.warwick.tabula.{CurrentUser, Features, FeaturesComponent, PersistenceTestBase}

class EditMeetingRecordCommandTest extends PersistenceTestBase with MeetingRecordTests {

	@Test
	def creatorEditMeeting() = withUser(creator.userId) {  withFakeTime(aprilFool) {

		val cmd = new EditMeetingRecordCommand(meeting)
		cmd.copyToCommand(meeting)
		cmd.title = "Updated title fools"
		cmd.maintenanceModeService = maintenanceModeService
		cmd.notificationService = notificationService
		cmd.scheduledNotificationService = scheduledNotificationService
		cmd.meetingRecordService = meetingRecordService
		cmd.features = emptyFeatures
		cmd.features.meetingRecordApproval = true
		cmd.features.attendanceMonitoringMeetingPointType = false

		notificationService.findActionRequiredNotificationsByEntityAndType[MeetingRecordRejectedNotification](meeting.approvals.get(0)) returns Seq()

		meeting = transactional { tx => cmd.apply() }
		meeting.title should be ("Updated title fools")

		meeting.isPendingApproval should be {true}
		val studentCurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)
		meeting.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting.approvals.get(0)) returns true
		meeting.pendingApprovalBy(studentCurrentUser) should be {true}
	}}

	@Test
	def meetingRecordWorkflow() = withUser("cuslaj") {  withFakeTime(aprilFool) {

		// Here is a story about the meeting record workflow ...
		// A student sees a meeting record with an inaccurate description. She tries to reject but forgets to add a comment
		val studentCurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)

		var approvalCmd = new ApproveMeetingRecordCommand(meeting, studentCurrentUser) with ApproveMeetingRecordState with MeetingRecordDaoComponent
			with ApproveMeetingRecordValidation with MonitoringPointMeetingRelationshipTermServiceComponent
			with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent with SecurityServiceComponent {
				val meetingRecordDao = smartMock[MeetingRecordDao]
				val monitoringPointMeetingRelationshipTermService = smartMock[MonitoringPointMeetingRelationshipTermService]
				val features = smartMock[Features]
				val attendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
				val securityService = smartMock[SecurityService]
			}

		approvalCmd.approved = false
		val errors = new BindException(approvalCmd, "command")
		approvalCmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("rejectionComments")
		errors.getFieldError.getCode should be ("meetingRecordApproval.rejectionComments.isEmpty")

		// Validation prompts them for a rejection comment. They rant about herons and reject.
		val heronRant = "There is no mention of herons in the meeting record. I distinctly remember LOADS of herons in my face."
		approvalCmd.rejectionComments = heronRant
		approvalCmd.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting.approvals.get(0)) returns true
		var approval = transactional { tx => approvalCmd.applyInternal() }.approvals.get(0)
		approval.state should be (Rejected)
		approval.comments should be (heronRant)
		meeting.isRejected should be {true}
		meeting.pendingRevisionBy(new CurrentUser(creator.asSsoUser, creator.asSsoUser)) should be {true}

		// The tutor sees the rejection. They add a description about herons to placate the student.
		val editCmd = new EditMeetingRecordCommand(meeting)
		editCmd.features = emptyFeatures
		editCmd.features.meetingRecordApproval = true
		editCmd.features.attendanceMonitoringMeetingPointType = false

		editCmd.meetingRecordService = meetingRecordService
		editCmd.notificationService = notificationService
		editCmd.scheduledNotificationService = scheduledNotificationService
		editCmd.copyToCommand(meeting)
		editCmd.description = "The meeting room was full of angry herons. It was truly harrowing."
		notificationService.findActionRequiredNotificationsByEntityAndType[MeetingRecordRejectedNotification](meeting.approvals.get(0)) returns Seq()
		val meeting2 = transactional { tx => editCmd.apply() }
		meeting2.isPendingApproval should be {true}
		meeting2.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting2.approvals.get(0)) returns true
		meeting2.pendingApprovalBy(studentCurrentUser) should be {true}

		// The student is now happy with the record and approves it
		approvalCmd = new ApproveMeetingRecordCommand(meeting, studentCurrentUser)
			with ApproveMeetingRecordState with MeetingRecordDaoComponent with SecurityServiceComponent
			with ApproveMeetingRecordValidation with MonitoringPointMeetingRelationshipTermServiceComponent
			with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent {
				val meetingRecordDao = mock[MeetingRecordDao]
				val monitoringPointMeetingRelationshipTermService = mock[MonitoringPointMeetingRelationshipTermService]
				val features = mock[Features]
				val attendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
				val securityService = smartMock[SecurityService]
			}
		approvalCmd.approved = true
		approvalCmd.rejectionComments = null
		approvalCmd.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, meeting2.approvals.get(0)) returns true
		approval = transactional { tx => approvalCmd.applyInternal() }.approvals.get(0)
		meeting2.isApproved should be {true}
		// Fin
	}}

}

package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.MeetingRecordApprovalType.{AllApprovals, OneApproval}
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord._
import uk.ac.warwick.tabula.data.model.{MeetingRecord, MeetingRecordApproval, Notification, SingleItemNotification}
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, FeaturesComponent}

import scala.collection.JavaConverters._

object ApproveMeetingRecordCommand {
	def apply(meeting: MeetingRecord, user: CurrentUser) =
		new ApproveMeetingRecordCommand(meeting, user)
		with AutowiringSecurityServiceComponent
		with AutowiringMeetingRecordDaoComponent
		with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
		with ComposableCommand[MeetingRecord]
		with ApproveMeetingRecordDescription
		with ApproveMeetingRecordPermission
		with ApproveMeetingRecordValidation
		with ApproveMeetingRecordNotification
		with ApproveMeetingRecordNotificationCompletion
}

class ApproveMeetingRecordCommand (val meeting: MeetingRecord, val user: CurrentUser)
	extends CommandInternal[MeetingRecord] with ApproveMeetingRecordState {

	self: MeetingRecordDaoComponent with FeaturesComponent
		with AttendanceMonitoringMeetingRecordServiceComponent with SecurityServiceComponent =>

	def applyInternal(): MeetingRecord = transactional() {
		if (approved) {
			approvals.foreach { approval =>
				approval.state = Approved
				user.profile.foreach(approval.approvedBy = _)
				approval.lastUpdatedDate = DateTime.now
				meetingRecordDao.saveOrUpdate(approval)
			}

			notRequiredApprovals.foreach { approval =>
				approval.state = NotRequired
				approval.lastUpdatedDate = DateTime.now
				meetingRecordDao.saveOrUpdate(approval)
			}
		} else {
			approvals.foreach { approval =>
				approval.state = Rejected
				approval.comments = rejectionComments
				approval.lastUpdatedDate = DateTime.now
				meetingRecordDao.saveOrUpdate(approval)
			}
		}

		if (features.attendanceMonitoringMeetingPointType) {
			attendanceMonitoringMeetingRecordService.updateCheckpoints(meeting)
		}

		meeting
	}
}

trait ApproveMeetingRecordValidation extends SelfValidating {
	self: ApproveMeetingRecordState =>

	def validate(errors: Errors) {
		if (meeting.deleted){
			errors.reject("meetingRecordApproval.meetingRecord.deleted")
		}
		if (meeting.isRejected) {
			errors.reject("meetingRecordApproval.meetingRecord.rejected")
		}
		if (approved == null) {
			errors.rejectValue("approved", "meetingRecordApproval.approved.isNull")
		} else if (!approved && !rejectionComments.hasText) {
			errors.rejectValue("rejectionComments", "meetingRecordApproval.rejectionComments.isEmpty")
		}
	}
}

trait ApproveMeetingRecordPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ApproveMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(meeting.approvals.asScala.map(approval =>
			CheckablePermission(Permissions.Profiles.MeetingRecord.Approve, approval)
		))
	}
}

trait ApproveMeetingRecordDescription extends Describable[MeetingRecord] {

	self: ApproveMeetingRecordState =>

	def describe(d: Description) {
		d.meeting(meeting)
		 .property("approved" -> approved)

		if (!approved && rejectionComments != null)
			d.property("rejectionComments", rejectionComments)
	}
}

trait ApproveMeetingRecordNotification extends Notifies[MeetingRecord, MeetingRecord] {
	self: ApproveMeetingRecordState =>

	def emit(meeting: MeetingRecord): Seq[Notification[MeetingRecordApproval, Unit] with MeetingRecordNotificationTrait with SingleItemNotification[MeetingRecordApproval]] = {
		val agent = user.apparentUser

		if (approved)
			Seq(Notification.init(new MeetingRecordApprovedNotification, agent, Seq(approvals.head)))
		else Seq( Notification.init(new MeetingRecordRejectedNotification, agent, Seq(approvals.head) ))
	}

}

trait ApproveMeetingRecordNotificationCompletion extends CompletesNotifications[MeetingRecord] {
	self: NotificationHandling with ApproveMeetingRecordState =>

	def notificationsToComplete(commandResult: MeetingRecord): CompletesNotificationsResult = {
		if (commandResult.isApproved) {
			CompletesNotificationsResult(
				notificationService.findActionRequiredNotificationsByEntityAndType[NewMeetingRecordApprovalNotification](commandResult) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[EditedMeetingRecordApprovalNotification](commandResult)
				,
				user.apparentUser
			)
		} else EmptyCompletesNotificationsResult
	}
}

trait ApproveMeetingRecordState {

	self: SecurityServiceComponent =>

	def meeting: MeetingRecord
	def user: CurrentUser
	var approved: JBoolean = _
	var rejectionComments: String =_

	lazy val approvals: Seq[MeetingRecordApproval] =
		meeting.approvals.asScala.filter(approval => securityService.can(user, Permissions.Profiles.MeetingRecord.Approve, approval))

	lazy val notRequiredApprovals: Seq[MeetingRecordApproval] = meeting.approvalType match {
		case AllApprovals => Nil
		case OneApproval => meeting.approvals.asScala -- approvals
	}
}

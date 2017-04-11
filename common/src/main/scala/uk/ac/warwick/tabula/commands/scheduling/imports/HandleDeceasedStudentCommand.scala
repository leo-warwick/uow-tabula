package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ExtensionRequestCreatedNotification, ExtensionRequestModifiedNotification}
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{EditedMeetingRecordApprovalNotification, MeetingRecordRejectedNotification, NewMeetingRecordApprovalNotification, ScheduledMeetingRecordConfirmNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.JavaConverters._

object HandleDeceasedStudentCommand {
	def apply(student: StudentMember) =
		new HandleDeceasedStudentCommandInternal(student)
			with AutowiringExtensionServiceComponent
			with AutowiringRelationshipServiceComponent
			with AutowiringMeetingRecordServiceComponent
			with NotificationServiceComponent
			with ScheduledNotificationServiceComponent
			with ComposableCommand[Unit]
			with HandleDeceasedStudentPermissions
			with Unaudited
}


class HandleDeceasedStudentCommandInternal(student: StudentMember) extends CommandInternal[Unit] {

	self: ExtensionServiceComponent with RelationshipServiceComponent with MeetingRecordServiceComponent
		with NotificationServiceComponent with ScheduledNotificationServiceComponent =>

	override def applyInternal(): Unit = {
		if (student.deceased) {
			cleanUpNotifications(handlePendingExtensions() ++ handleRelationships())
		}
	}

	private def handlePendingExtensions(): Seq[Extension] = {
		val extensions = extensionService.getPreviousExtensions(MemberOrUser(student).asUser).filter(_.awaitingReview)
		extensions.foreach(_.reject("Automatically removing request from deceased student"))
		extensions.foreach(extensionService.saveOrUpdate)
		extensions
	}

	private def handleRelationships(): Seq[ToEntityReference] = {
		val current = relationshipService.getAllCurrentRelationships(student)
		val future = relationshipService.getAllFutureRelationships(student)
		current.filter(rel => Option(rel.endDate).exists(_.isAfterNow)).foreach { rel =>
			rel.endDate = null
			relationshipService.saveOrUpdate(rel)
		}
		relationshipService.removeFutureStudentRelationships(future)
		current ++ future ++ handleMeetingRecords(current)
	}

	private def handleMeetingRecords(relationships: Seq[StudentRelationship]): Seq[AbstractMeetingRecord] = {
		relationships.flatMap(rel =>
			rel.agentMember.map(MemberOrUser(_).asUser).map(agentUser =>
				meetingRecordService.listAll(rel).filter(abstractMeeting => HibernateHelpers.initialiseAndUnproxy(abstractMeeting) match {
					case meeting: MeetingRecord => meeting.pendingActionBy(new CurrentUser(agentUser, agentUser))
					case scheduled: ScheduledMeetingRecord => scheduled.creator == rel.agentMember.get
				})
			).getOrElse(Seq())
		)
	}

	private def completeNotification(completedBy: User)(notification: ActionRequiredNotification): Unit = {
		notification match {
			case _: AllCompletedActionRequiredNotification => notification.actionCompleted(completedBy)
			case recipient: RecipientCompletedActionRequiredNotification => recipient.actionCompletedByOther(completedBy)
			case _ => throw new IllegalArgumentException
		}
	}

	private def cleanUpNotifications(entities: Seq[ToEntityReference]): Unit = {
		val user = new AnonymousUser()

		val notifications: Seq[ActionRequiredNotification] = entities.flatMap {

			case extension: Extension =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestCreatedNotification](extension) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestModifiedNotification](extension)

			case meeting: MeetingRecord =>
				meeting.approvals.asScala.flatMap(approval => notificationService.findActionRequiredNotificationsByEntityAndType[MeetingRecordRejectedNotification](approval)) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[NewMeetingRecordApprovalNotification](meeting) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[EditedMeetingRecordApprovalNotification](meeting)

			case scheduledMeeting: ScheduledMeetingRecord =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ScheduledMeetingRecordConfirmNotification](scheduledMeeting)

			case _ =>
				Seq()
		}

		if (notifications.nonEmpty) {
			notifications.foreach(completeNotification(user))
			notificationService.update(
				notifications.map(_.asInstanceOf[Notification[_, _]]),
				user
			)
		}

		entities.foreach(scheduledNotificationService.removeInvalidNotifications)
	}

}

trait HandleDeceasedStudentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ScheduledNotificationService, NotificationService}
import uk.ac.warwick.tabula.commands.{CompletesNotifications, SchedulesNotifications, Notifies, Command}
import uk.ac.warwick.tabula.jobs.{Job, NotifyingJob}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging

trait NotificationHandling extends Logging {

	var notificationService: NotificationService = Wire[NotificationService]
	var scheduledNotificationService: ScheduledNotificationService = Wire[ScheduledNotificationService]

	def notify[A, B, C](cmd: Command[A])(f: => A): A = {
		val result = f

		cmd match {
			case ns: Notifies[A @unchecked, B @unchecked] =>
				for (notification <- ns.emit(result)) {
					notificationService.push(notification)
				}
			case _ =>
		}

		cmd match {
			case sn: SchedulesNotifications[A @unchecked, C @unchecked] =>

				val notificationTargets = sn.transformResult(result)

				for (target <- notificationTargets) {
					scheduledNotificationService.removeInvalidNotifications(target)

					for (scheduledNotification <- sn.scheduledNotifications(target)) {
						if (scheduledNotification.scheduledDate.isBeforeNow) {
							logger.warn("ScheduledNotification generated in the past, ignoring: " + scheduledNotification)
						} else {
							scheduledNotificationService.push(scheduledNotification)
						}
					}
				}
			case _ =>
		}

		cmd match {
			case ns: CompletesNotifications[A @unchecked] =>
				val notificationResult = ns.notificationsToComplete(result)
				for (notification <- notificationResult.notifications) {
					notification.actionCompleted(notificationResult.completedBy)
				}

				if (notificationResult.notifications.nonEmpty) {
					notificationService.update(
						notificationResult.notifications.map(_.asInstanceOf[Notification[_, _]]),
						notificationResult.completedBy
					)
				}
			case _ =>
		}

		result
	}

	/**
	 * For edge cases where manual notifications need to be made outside commands.
	 * Use the command-triggered mixin above where possible for better type safety.
	 */
	def notify[A](notifications: Seq[Notification[_, _]]) {
		notifications.foreach { n => notificationService.push(n) }
	}
}

trait JobNotificationHandling {

	var notificationService: NotificationService = Wire.auto[NotificationService]

	def notify[A](instance: JobInstance, job: Job) {
		job match {
			case ns: NotifyingJob[A @unchecked] => for (notification <- ns.popNotifications(instance)) {
				notificationService.push(notification)
			}
			case _ => // do nothing. This job doesn't notify
		}
	}
}

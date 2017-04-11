package uk.ac.warwick.tabula.services

import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{CanBeDeleted, Notification, ScheduledNotification, ToEntityReference}
import uk.ac.warwick.tabula.data.{Daoisms, ScheduledNotificationDao}
import uk.ac.warwick.tabula.helpers.{Logging, ReflectionHelper}
import uk.ac.warwick.userlookup.AnonymousUser

trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_])
	def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) : Option[Notification[_,_]]
	def processNotifications()
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService with Logging with Daoisms {

	val RunBatchSize = 10

	var dao: ScheduledNotificationDao = Wire.auto[ScheduledNotificationDao]
	var notificationService: NotificationService = Wire.auto[NotificationService]

	// a map of DiscriminatorValue -> Notification
	lazy val notificationMap: Map[String, Class[_ <: Notification[ToEntityReference, Unit]]] = ReflectionHelper.allNotifications

	override def push(sn: ScheduledNotification[_]): Unit = dao.save(sn)

	override def removeInvalidNotifications(target: Any): Unit = {
		val existingNotifications = dao.getScheduledNotifications(target)
		existingNotifications.foreach(dao.delete)
	}

	override def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]): Option[Notification[ToEntityReference, Unit]] = {
		try {
			val notificationClass = notificationMap(sn.notificationType)
			val baseNotification: Notification[ToEntityReference, Unit] = notificationClass.newInstance()
			sn.target.entity match {
				case entity: CanBeDeleted if entity.deleted => None
				case entity => Some(Notification.init(baseNotification, new AnonymousUser, entity))
			}
		} catch {
			// Can happen if reference to an entity has since been deleted, e.g.
			// a submission is resubmitted and the old submission is removed. Skip this notification.
			case onf: ObjectNotFoundException =>
				debug("Skipping scheduled notification %s as a referenced object was not found", sn)
				None
		}
	}

	/**
	 * This is called peridoically to convert uncompleted ScheduledNotifications into real instances of notification.
	 */
	override def processNotifications(): Unit = {
		val ids = transactional(readOnly = true) { dao.notificationsToComplete.take(RunBatchSize).map[String] { _.id }.toList }

		// FIXME we are doing this manually (TAB-2221) because Hibernate keeps failing to do this properly. Importantly, we're not
		// using notificationService.push, which is dangerous
		ids.foreach { id =>
			inSession { session =>
				transactional(readOnly = true) { // Some things that use notification require a read-only session to be bound to the thread
					Option(session.get(classOf[ScheduledNotification[_]], id)).foreach {
						rawSn =>
							val sn = rawSn.asInstanceOf[ScheduledNotification[_ >: Null <: ToEntityReference]]

							logger.info(s"Processing scheduled notification $sn")
							// Even if we threw an error above and didn't actually push a notification, still mark it as completed
							sn.completed = true
							session.saveOrUpdate(sn)

							val notification = generateNotification(sn)
							notification.foreach { notification =>
								logger.info("Notification pushed - " + notification)
								notification.preSave(newRecord = true)
								session.saveOrUpdate(notification)
							}

							session.flush()
					}
				}
			}
		}
	}
}

trait ScheduledNotificationServiceComponent {
	def scheduledNotificationService: ScheduledNotificationService
}

trait AutowiringScheduledNotificationServiceComponent extends ScheduledNotificationServiceComponent {
	var scheduledNotificationService: ScheduledNotificationService = Wire[ScheduledNotificationService]
}

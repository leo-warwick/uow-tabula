package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{CanBeDeleted, ToEntityReference, Notification, ScheduledNotification}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{MaintenanceModeAwareSession, ScheduledNotificationDao}
import uk.ac.warwick.tabula.helpers.{Logging, ReflectionHelper}
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.Transactions._
import org.hibernate.{SessionFactory, ObjectNotFoundException}

trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_ >: Null <: ToEntityReference])
	def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) : Option[Notification[_,_]]
	def processNotifications()
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService with Logging
	with AutowiringNotificationServiceComponent {

	val RunBatchSize = 10

	var dao = Wire[ScheduledNotificationDao]
	var sessionFactory = Wire[SessionFactory]

	// a map of DiscriminatorValue -> Notification
	lazy val notificationMap = ReflectionHelper.allNotifications

	override def push(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) = dao.save(sn)

	override def removeInvalidNotifications(target: Any) = {
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

	private def inSession(fn: MaintenanceModeAwareSession => Unit) {
		val sess = MaintenanceModeAwareSession(sessionFactory.openSession())
		try fn(sess) finally sess.close()
	}

	/**
	 * This is called peridoically to convert uncompleted ScheduledNotifications into real instances of notification.
	 */
	override def processNotifications() = {
		val ids = transactional(readOnly = true) { dao.notificationsToComplete.take(RunBatchSize).map[String] { _.id }.toList }

		// FIXME we are doing this manually (TAB-2221) because Hibernate keeps failing to do this properly. Importantly, we're not
		// using notificationService.push, which is dangerous
		ids.foreach { id =>
			inSession { session =>
				transactional(readOnly = true) { // Some things that use notification require a read-only session to be bound to the thread
					session.getById[ScheduledNotification[_]](id).foreach {
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

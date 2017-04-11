package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{ActionRequiredNotification, Notification, ToEntityReference}

import scala.reflect.ClassTag

object NotificationDao {
	// if a notification fails to send don't retry for this many minutes
	val RETRY_DELAY_MINUTES = 5
}

trait NotificationDao {
	def save(notification: Notification[_,_])
	def save(recipientInfo: RecipientNotificationInfo)

	def update(notification: Notification[_,_])

	def getById(id: String): Option[Notification[_  >: Null <: ToEntityReference, _]]
	def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification]

	def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference,_]]
	def unemailedRecipientCount: Number
	def unemailedRecipients: Scrollable[RecipientNotificationInfo]
	def oldestUnemailedRecipient: Option[RecipientNotificationInfo]
	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo]

	def unprocessedNotificationCount: Number
	def unprocessedNotifications: Scrollable[Notification[_ >: Null <: ToEntityReference, _]]
}

@Repository
class NotificationDaoImpl extends NotificationDao with Daoisms {

	/** A Scrollable of all notifications since this date, sorted date ascending.
		*/
	def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference,_]] = {
		val scrollable = session.newCriteria[Notification[_ >: Null <: ToEntityReference,_]]
			.add(Restrictions.ge("created", start))
			.addOrder(Order.asc("created"))
			.scroll()
		Scrollable(scrollable, session)
	}

	private def unemailedRecipientCriteria = {

		val notAttemptedRecently = disjunction()
			.add(isNull("attemptedAt"))
			.add(lt("attemptedAt", DateTime.now.minusMinutes(NotificationDao.RETRY_DELAY_MINUTES)))

		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.add(is("emailSent", false))
			.add(is("dismissed", false))
			.add(notAttemptedRecently)
	}

	def unemailedRecipientCount: Number =
		unemailedRecipientCriteria.count

	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = {
		val scrollable = unemailedRecipientCriteria
			.addOrder(Order.asc("notification.created"))
			.scroll()
		Scrollable(scrollable, session)
	}

	def oldestUnemailedRecipient: Option[RecipientNotificationInfo] = {
		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.add(is("emailSent", false))
			.add(is("dismissed", false))
			.addOrder(Order.asc("notification.created"))
			.setMaxResults(1)
			.seq.headOption
	}

	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] =
		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.setFetchMode("notification", FetchMode.JOIN)
			.add(Restrictions.disjunction(is("emailSent", true), is("dismissed", false)))
			.addOrder(Order.asc("emailSent"))
			.addOrder(Order.desc("notification.created"))
			.setFirstResult(start)
			.setMaxResults(count)
			.seq

	def save(notification: Notification[_,_]) {
		/**
		 * FIXME This should no longer be required but submitting assignments
		 * doesn't work without it.
		 *
		 * PreSaveBehaviour usually doesn't happen until flush, but we need
		 * properties to be set before flush to avoid ConcurrentModificationExceptions.
		 *
		 * There are other pre-flush Hibernate event types we could create listeners for.
		 */
		val isNew = notification.id == null
		notification.preSave(isNew)

		session.save(notification)
		session.flush() // TAB-2381
	}

	def save(recipientInfo: RecipientNotificationInfo) {
		session.saveOrUpdate(recipientInfo)
	}

	def update(notification: Notification[_,_]) {
		session.saveOrUpdate(notification)
	}

	def getById(id: String): Option[Notification[_ >: Null <: ToEntityReference, _]] = getById[Notification[_ >: Null <: ToEntityReference,_]](id)

	def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification] = {
		val targetEntity = entity match {
			case ref: ToEntityReference => ref.toEntityReference.entity
			case _ => entity
		}
		session.newCriteria[A]
			.createAlias("items", "items")
			.add(is("items.entity", targetEntity))
			.seq
	}

	private def unprocessedNotificationCriteria =
		session.newCriteria[Notification[_ >: Null <: ToEntityReference, _]]
			.add(is("_listenersProcessed", false))

	def unprocessedNotificationCount: Number =
		unprocessedNotificationCriteria.count

	def unprocessedNotifications: Scrollable[Notification[_ >: Null <: ToEntityReference, _]] = {
		val scrollable = unprocessedNotificationCriteria
			.addOrder(Order.asc("created"))
			.scroll()
		Scrollable(scrollable, session)
	}
}

trait NotificationDaoComponent {
	def notificationDao: NotificationDao
}

trait AutowiringNotificationDaoComponent extends NotificationDaoComponent {
	var notificationDao: NotificationDao = Wire[NotificationDao]
}
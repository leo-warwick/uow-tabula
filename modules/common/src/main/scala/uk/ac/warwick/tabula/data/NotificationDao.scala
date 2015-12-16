package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{ToEntityReference, ActionRequiredNotification, Notification}

import scala.reflect.ClassTag

trait NotificationDao {
	def save(notification: Notification[_ >: Null <: ToEntityReference, _])
	def save(recipientInfo: RecipientNotificationInfo)

	def update(notification: Notification[_ >: Null <: ToEntityReference, _])

	def getById(id: String): Option[Notification[_  >: Null <: ToEntityReference, _]]
	def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification]

	def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference, _]]
	def unemailedRecipientCount: Number
	def unemailedRecipients: Scrollable[RecipientNotificationInfo]
	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo]

	def flush(): Unit
}

@Repository
class NotificationDaoImpl extends NotificationDao with Daoisms {

	private def idFunction(notification: Notification[_ >: Null <: ToEntityReference, _]) = notification.id

	/** A Scrollable of all notifications since this date, sorted date ascending.
		*/
	def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference, _]] = {
		val scrollableResults = session.newCriteria[Notification[_ >: Null <: ToEntityReference, _]]
			.add(Restrictions.ge("created", start))
			.addOrder(Order.asc("created"))
			.scroll()
		session.scrollable(scrollableResults)
	}

	private def unemailedRecipientCriteria =
		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.add(is("emailSent", false))
			.add(is("dismissed", false))

	def unemailedRecipientCount =
		unemailedRecipientCriteria.count

	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = {
		val scrollableResults = unemailedRecipientCriteria
			.addOrder(Order.asc("notification.created"))
			.scroll()
		session.scrollable(scrollableResults)
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

	def save(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
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

	def update(notification: Notification[_ >: Null <: ToEntityReference, _]) {
		session.saveOrUpdate(notification)
	}

	def getById(id: String) = session.getById[Notification[_ >: Null <: ToEntityReference,_]](id)

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

	def flush() = session.flush()
}

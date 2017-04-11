package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.{ScheduledNotification, ToEntityReference}
import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime

trait ScheduledNotificationDao {

		def save(scheduledNotification: ScheduledNotification[_]): Unit

		def delete(scheduledNotification: ScheduledNotification[_]): Unit

		def getById(id: String): Option[ScheduledNotification[_  >: Null <: ToEntityReference]]

		def notificationsToComplete: Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]]

		def getScheduledNotifications(entity: Any): Seq[ScheduledNotification[_  >: Null <: ToEntityReference]]

}

@Repository
class ScheduledNotificationDaoImpl extends ScheduledNotificationDao with Daoisms {

	override def save(scheduledNotification: ScheduledNotification[_]): Unit = {
		session.saveOrUpdate(scheduledNotification)
	}

	override def getById(id: String): Option[ScheduledNotification[_ >: Null <: ToEntityReference]] = getById[ScheduledNotification[_ >: Null <: ToEntityReference]](id)

	override def getScheduledNotifications(entity: Any): Seq[ScheduledNotification[_ >: Null <: ToEntityReference]] = {
		val targetEntity = entity match {
			case ref: ToEntityReference => ref.toEntityReference.entity
			case _ => entity
		}
		session.newCriteria[ScheduledNotification[_  >: Null <: ToEntityReference]]
			.createAlias("target", "target")
			.add(Restrictions.eq("target.entity", targetEntity))
			.add(Restrictions.ne("completed", true))
			.addOrder(Order.asc("scheduledDate"))
			.seq
	}

	override def delete(scheduledNotification: ScheduledNotification[_]): Unit = session.delete(scheduledNotification)

	override def notificationsToComplete: Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]] = {
		val scrollable =
			session.newCriteria[ScheduledNotification[_  >: Null <: ToEntityReference]]
			.add(Restrictions.ne("completed", true))
			.add(Restrictions.le("scheduledDate", DateTime.now))
			.addOrder(Order.asc("scheduledDate"))
			.scroll()
		Scrollable(scrollable, session)
	}
}
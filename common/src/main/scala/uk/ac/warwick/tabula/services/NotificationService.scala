package uk.ac.warwick.tabula.services

import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{Daoisms, NotificationDao}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.services.elasticsearch.{IndexedNotification, NotificationIndexService, NotificationQueryService}
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.userlookup.User

import scala.reflect.ClassTag

case class ActivityStreamRequest(
	user: User,
	max: Int = 100,
	priority: Double = 0,
	includeDismissed: Boolean = false,
	types: Option[Set[String]] = None,
	lastUpdatedDate: Option[DateTime]
)

case class ActivityStream(
	items: Seq[Activity[Any]],
	lastUpdatedDate: Option[DateTime],
	totalHits: Long
)

@Service
class NotificationService extends Logging with FreemarkerTextRenderer with Daoisms {

	var dao: NotificationDao = Wire[NotificationDao]
	var userLookup: UserLookupService = Wire[UserLookupService]
	var queryService: NotificationQueryService = Wire[NotificationQueryService]
	var indexService: NotificationIndexService = Wire[NotificationIndexService]
	var listeners: Seq[NotificationListener] = Wire.all[NotificationListener]

	def getNotificationById(id: String): Option[Notification[_ >: Null <: ToEntityReference, _]] = dao.getById(id)

	def push(notification: Notification[_,_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		dao.save(notification)

		indexService.indexItems(notification.recipients.toList.map { user => IndexedNotification(notification.asInstanceOf[Notification[_ >: Null <: ToEntityReference, _]], user) })

		// Individual listeners are responsible for pulling notifications
	}

	// update the notifications and rebuild their entries index
	def update(notifications: Seq[Notification[_, _]], user: User) {
		notifications.foreach(dao.update)

		indexService.indexItems(notifications.map { n => IndexedNotification(n.asInstanceOf[Notification[_ >: Null <: ToEntityReference, _]], user) })
	}

	def stream(req: ActivityStreamRequest): ActivityStream = {
		val notifications = queryService.userStream(req)
		val activities = notifications.items.flatMap(toActivity)

		ActivityStream(
			items = activities,
			lastUpdatedDate = notifications.lastUpdatedDate,
			totalHits = notifications.totalHits
		)
	}

	def toActivity(notification: Notification[_, _]) : Option[Activity[Any]] = {
		try {
			val (message, priority) =
				notification match {
					case actionRequired: ActionRequiredNotification if actionRequired.completed =>
						val actionRequired = notification.asInstanceOf[ActionRequiredNotification]
						val message = "Completed by %s on %s".format(
							userLookup.getUserByUserId(actionRequired.completedBy) match {
								case FoundUser(u) => u.getFullName
								case _ => "Unknown user"
							},
							notification.dateTimeFormatter.print(actionRequired.completedOn)
						)
						val priority = NotificationPriority.Complete
						(message, priority)

					case _ =>
						val content = notification.content
						val message = renderTemplate(content.template, content.model)
						val priority = notification.priorityOrDefault
						(message, priority)
				}

			Some(new Activity[Any](
				id = notification.id,
				title = notification.title,
				date = notification.created,
				priority = priority.toNumericalValue,
				agent = notification.agent,
				verb = notification.verb,
				url = notification.url,
				urlTitle = notification.urlTitle,
				message = message,
				entity = null
			))
		} catch {
			// referenced entity probably missing, oh well.
			case e: ObjectNotFoundException => None
		}
	}

	def save(recipientInfo: RecipientNotificationInfo): Unit = transactional() {
		dao.save(recipientInfo)
	}

	def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification] = {
		dao.findActionRequiredNotificationsByEntityAndType[A](entity)
	}

	def processListeners(): Unit = transactional() {
		dao.unprocessedNotifications.take(NotificationService.ProcessListenersBatchSize).foreach { notification =>
			try {
				logger.info("Processing notification listeners - " + notification)
				listeners.foreach { _.listen(notification) }
				notification.markListenersProcessed()
				dao.update(notification)
				session.flush()
			} catch {
				case throwable: Throwable => {
					// TAB-2238 Catch and log, so that the overall transaction can still commit
					logger.error("Exception handling notification listening:", throwable)
				}
			}
		}
	}

}

object NotificationService {
	val ProcessListenersBatchSize = 100
}

trait NotificationListener {
	def listen(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit
}

trait RecipientNotificationListener {
	def listen(recipient: RecipientNotificationInfo): Unit
}

trait NotificationServiceComponent {
	def notificationService: NotificationService
}

trait AutowiringNotificationServiceComponent extends NotificationServiceComponent {
	var notificationService: NotificationService = Wire[NotificationService]
}
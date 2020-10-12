package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.mappings.FieldDefinition
import javax.persistence.DiscriminatorValue
import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Identifiable, Notification, ToEntityReference}
import uk.ac.warwick.tabula.data.{NotificationDao, NotificationDaoComponent}
import uk.ac.warwick.userlookup.User

import scala.language.existentials

case class IndexedNotification(notification: Notification[_ >: Null <: ToEntityReference, _], recipient: User) extends Identifiable {
  def id = s"${notification.id}-${recipient.getUserId}"
}

object NotificationIndexService {

  implicit object IndexedNotificationIndexable extends ElasticsearchIndexable[IndexedNotification] {
    override def fields(item: IndexedNotification): Map[String, Any] = {
      val recipient = item.recipient
      val notification = item.notification

      val notificationType = notification.getClass.getAnnotation(classOf[DiscriminatorValue]).value()
      val priority = notification.priorityOrDefault

      Map(
        "notification" -> notification.id,
        "recipient" -> recipient.getUserId,
        "notificationType" -> notificationType,
        "priority" -> priority.toNumericalValue,
        "dismissed" -> notification.isDismissed(recipient),
        "created" -> DateFormats.IsoDateTime.print(notification.created)
      )
    }

    override def lastUpdatedDate(item: IndexedNotification): DateTime = item.notification.created
  }

}

@Service
class NotificationIndexService
  extends AbstractIndexService[IndexedNotification]
    with NotificationDaoComponent
    with NotificationElasticsearchConfig {

  override implicit val indexable: ElasticsearchIndexable[IndexedNotification] = NotificationIndexService.IndexedNotificationIndexable

  /**
    * The name of the index that this service writes to
    */
  @Value("${elasticsearch.index.notifications.name}") var indexName: String = _
  lazy val index: Index = Index(indexName)

  @Autowired var notificationDao: NotificationDao = _

  final override val IncrementalBatchSize: Int = 5000

  override val UpdatedDateField: String = "created"

  override protected def listNewerThan(startDate: DateTime, batchSize: Int): List[IndexedNotification] = transactional(readOnly = true) {
    notificationDao.recent(startDate).take(batchSize).flatMap { notification =>
      try {
        notification.recipients.toList.map { user => IndexedNotification(notification, user) }
      } catch {
        // Can happen if reference to an entity has since been deleted, e.g.
        // a submission is resubmitted and the old submission is removed. Skip this notification.
        case _: ObjectNotFoundException =>
          debug("Skipping notification %s as a referenced object was not found", notification)
          Nil
      }
    }.filter { notification =>
      val recipient = notification.recipient
      recipient.isFoundUser && recipient.getUserId != null
    }.toList
  }

}

trait NotificationElasticsearchConfig extends ElasticsearchConfig {
  override def fields: Seq[FieldDefinition] = Seq(
    doubleField("priority"),
    booleanField("dismissed"),
    keywordField("notificationType"),
    dateField("created").format("strict_date_time_no_millis")
  )
}

package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.{Notification, ToEntityReference}
import uk.ac.warwick.tabula.data.{NotificationDao, NotificationDaoComponent}
import uk.ac.warwick.tabula.services.ActivityStreamRequest

import scala.concurrent.duration._

case class PagedNotifications(items: Seq[Notification[_ >: Null <: ToEntityReference, _]], lastUpdatedDate: Option[DateTime], totalHits: Long)

trait NotificationQueryService
  extends NotificationQueryMethods

trait NotificationQueryMethods {
  def userStream(req: ActivityStreamRequest): PagedNotifications
}

@Service
class NotificationQueryServiceImpl extends AbstractQueryService
  with NotificationQueryService
  with NotificationQueryMethodsImpl
  with NotificationDaoComponent {

  /**
    * The name of the index alias that this service reads from
    */
  @Value("${elasticsearch.index.notifications.alias}") var indexName: String = _
  lazy val index: Index = Index(indexName)

  @Autowired var notificationDao: NotificationDao = _

}

trait NotificationQueryMethodsImpl extends NotificationQueryMethods {
  self: ElasticsearchClientComponent
    with ElasticsearchSearching
    with NotificationDaoComponent =>

  private def toNotifications(hits: Iterable[SearchHit]): Seq[Notification[_ >: Null <: ToEntityReference, _]] =
    hits.flatMap { hit =>
      notificationDao.getById(hit.sourceAsMap("notification").toString)
    }.toSeq

  override def userStream(req: ActivityStreamRequest): PagedNotifications = {
    val recipientQuery = Some(termQuery("recipient", req.user.getUserId))
    val priorityQuery = Some(rangeQuery("priority").gte(req.priority).lte(1.0))

    val dismissedQuery =
      if (req.includeDismissed) None
      else Some(termQuery("dismissed", false))

    val typesQuery = req.types.map {
      case types if types.size == 1 => termQuery("notificationType", types.head)
      case types => boolQuery().should(types.map { t => termQuery("notificationType", t) })
    }

    val pagingQuery = req.lastUpdatedDate.map { date =>
      rangeQuery("created").lt(DateFormats.IsoDateTime.print(date))
    }

    val query = boolQuery().must(Seq(recipientQuery, priorityQuery, dismissedQuery, typesQuery, pagingQuery).flatten)

    // Avoid Hibernate horror by waiting for the Future here, then initialising in the main thread
    val response =
      client.execute {
        searchRequest.query(query)
          .sortBy(fieldSort("created").order(SortOrder.Desc))
          .limit(req.max)
      }.await(10.seconds)

    val items = toNotifications(response.result.hits.hits)

    PagedNotifications(
      items = items,
      lastUpdatedDate = items.lastOption.map(_.created),
      totalHits = response.result.totalHits
    )
  }
}

package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.RichSearchHit
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.{NotificationDao, NotificationDaoComponent}
import uk.ac.warwick.tabula.services.ActivityStreamRequest

import scala.concurrent.duration._

case class PagedNotifications(items: Seq[Notification[_, _]], lastUpdatedDate: Option[DateTime], totalHits: Long)

trait NotificationQueryService
	extends NotificationQueryMethods

trait NotificationQueryMethods {
	def userStream(req: ActivityStreamRequest): PagedNotifications
}

@Service
class NotificationQueryServiceImpl extends AbstractQueryService
	with NotificationIndexType
	with NotificationQueryService
	with NotificationQueryMethodsImpl
	with NotificationDaoComponent {

	/**
		* The name of the index alias that this service reads from
		*/
	@Value("${elasticsearch.index.notifications.alias}") var indexName: String = _

	@Autowired var notificationDao: NotificationDao = _

}

trait NotificationQueryMethodsImpl extends NotificationQueryMethods {
	self: ElasticsearchClientComponent
		with ElasticsearchSearching
		with NotificationDaoComponent =>

	private def toNotifications(hits: Iterable[RichSearchHit]): Seq[Notification[_, _]] =
		hits.flatMap { hit =>
			notificationDao.getById(hit.sourceAsMap("notification").toString)
		}.toSeq

	override def userStream(req: ActivityStreamRequest): PagedNotifications = {
		val recipientQuery = Some(termQuery("recipient", req.user.getUserId))
		val priorityQuery = Some(rangeQuery("priority") from req.priority to 1.0)

		val dismissedQuery =
			if (req.includeDismissed) None
			else Some(termQuery("dismissed", false))

		val typesQuery = req.types.map {
			case types if types.size == 1 => termQuery("notificationType", types.head)
			case types => bool { should(types.map { t => termQuery("notificationType", t) }) }
		}

		val pagingQuery = req.lastUpdatedDate.map { date =>
			rangeQuery("created") lte DateFormats.IsoDateTime.print(date) includeUpper false
		}

		val query = bool { must(Seq(recipientQuery, priorityQuery, dismissedQuery, typesQuery, pagingQuery).flatten) }

		// Avoid Hibernate horror by waiting for the Future here, then initialising in the main thread
		val results =
			client.execute { searchFor query query sort (field sort "created" order SortOrder.DESC) limit req.max }
				.await(10.seconds)

		val items = toNotifications(results.hits)

		PagedNotifications(
			items = items,
			lastUpdatedDate = items.lastOption.map { _.created },
			totalHits = results.totalHits
		)
	}
}
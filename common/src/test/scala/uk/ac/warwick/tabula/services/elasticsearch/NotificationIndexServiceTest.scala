package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import org.joda.time.DateTime
import org.junit.After
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.immutable.IndexedSeq

class NotificationIndexServiceTest extends ElasticsearchTestBase with Mockito {

  val index = Index("notification")

  private trait Fixture {
    val dao: NotificationDao = smartMock[NotificationDao]

    // default behaviour, we add individual expectations later
    dao.getById(any[String]) returns None

    val indexer = new NotificationIndexService
    indexer.indexName = NotificationIndexServiceTest.this.index.name
    indexer.client = NotificationIndexServiceTest.this.client
    indexer.notificationDao = dao

    // Creates the index
    indexer.ensureIndexExists().await should be(true)

    implicit val indexable = NotificationIndexService.IndexedNotificationIndexable
  }

  private trait DataFixture extends Fixture {
    val agent: User = Fixtures.user(userId = "abc")
    val recipient: User = Fixtures.user(userId = "xyz")
    val otherRecipient: User = Fixtures.user(userId = "xyo")

    val staff = Fixtures.staff("1234567")
    val student = Fixtures.student("9876543")
    val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

    val meeting = new MeetingRecord
    meeting.creator = staff

    val relationship = StudentRelationship(staff, relType, student, DateTime.now)
    meeting.relationships = Seq(relationship)

    val now: DateTime = DateTime.now

    // Selection of notifications intended for a couple of different recipients
    lazy val items: IndexedSeq[IndexedNotification] = for (i <- 1 to 100) yield {
      val notification =
        if (i % 2 == 0) {
          new HeronWarningNotification
        } else {
          new HeronDefeatedNotification
        }
      notification.id = "nid" + i
      notification.created = now.plusMinutes(i)
      dao.getById(notification.id) returns Some(notification)

      notification.priority = if (i <= 40) {
        NotificationPriority.Info
      } else if (i <= 80) {
        NotificationPriority.Warning
      } else {
        NotificationPriority.Critical
      }

      val theRecipient = if (i % 2 == 0) {
        recipient
      } else {
        otherRecipient
      }
      IndexedNotification(notification, theRecipient)
    }

    // The IDs of notifications we expect our recipient to get.
    lazy val recipientNotifications: IndexedSeq[IndexedNotification] = items.filter(_.recipient == recipient)
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
  }

  @Test def fields(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val notification = new HeronDefeatedNotification
      notification.id = "defeat"
      notification.created = DateTime.now()
      notification.priority = NotificationPriority.Info

      val recipient: User = Fixtures.user(userId = "xyz")

      val item = IndexedNotification(notification, recipient)

      indexer.indexItems(Seq(item)).await
      blockUntilExactCount(1, index.name)

      // University ID is the ID field so it isn't in the doc source
      val doc: GetResponse = client.execute {
        get(index, item.id)
      }.futureValue.result

      doc.source should be(Map(
        "notification" -> "defeat",
        "recipient" -> "xyz",
        "notificationType" -> "HeronDefeat",
        "priority" -> 0.25,
        "dismissed" -> false,
        "created" -> DateFormats.IsoDateTime.print(DateTime.now)
      ))
    }
  }

  @Test def indexItems(): Unit = new DataFixture {
    indexer.indexItems(items)
    blockUntilExactCount(100, index.name)

    val dates: Seq[DateTime] = client.execute {
      search(index).query(termQuery("recipient", "xyz")).sortBy(fieldSort("created").order(SortOrder.Desc)).limit(100)
    }.map {
      _.result.hits.hits.map { hit => DateFormats.IsoDateTime.parseDateTime(hit.sourceAsMap("created").toString) }.toSeq
    }
      .futureValue

    dates.size should be(50)
    dates.head should be(recipientNotifications.map(_.notification.created.withMillisOfSecond(0)).max)
  }

  @Test def missingRecipient(): Unit = new DataFixture {
    val anonUser = new AnonymousUser()
    val notification: HeronWarningNotification = Notification.init(new HeronWarningNotification, agent, meeting)
    indexer.indexItems(Seq(IndexedNotification(notification, anonUser))).await
    // No exception, good times.
  }

}

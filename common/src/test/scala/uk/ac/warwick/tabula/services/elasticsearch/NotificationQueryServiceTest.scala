package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import org.joda.time.DateTime
import org.junit.{After, Before}
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ActivityStreamRequest
import uk.ac.warwick.tabula.{ElasticsearchTestBase, Fixtures, Mockito}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.IndexedSeq

class NotificationQueryServiceTest extends ElasticsearchTestBase with Mockito {

  val index = Index("notifications")

  private trait Fixture {
    val queryService = new NotificationQueryServiceImpl
    queryService.notificationDao = smartMock[NotificationDao]
    queryService.client = NotificationQueryServiceTest.this.client
    queryService.indexName = NotificationQueryServiceTest.this.index.name

    implicit val indexable = NotificationIndexService.IndexedNotificationIndexable
  }

  private trait IndexedDataFixture extends Fixture {
    val agent: User = Fixtures.user(userId = "abc")
    val recipient: User = Fixtures.user(userId = "xyz")
    val otherRecipient: User = Fixtures.user(userId = "xyo")

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

    lazy val dismissedItem: IndexedNotification = {
      val staff = Fixtures.staff("1234567", userId = recipient.getUserId)
      val student = Fixtures.student("9876543")
      val relType = StudentRelationshipType("tutor", "tutor", "tutor", "tutor")

      val meeting = new MeetingRecord
      meeting.creator = staff

      val relationship = StudentRelationship(staff, relType, student, DateTime.now)
      meeting.relationships = Seq(relationship)

      val notification = Notification.init(new HeronWarningNotification, agent, meeting)
      notification.id = "nid101"
      notification.created = now.plusMinutes(101)
      notification.dismiss(recipient)
      IndexedNotification(notification, recipient)
    }

    (items :+ dismissedItem).foreach { item =>
      queryService.notificationDao.getById(item.notification.id) returns Some(item.notification)
      client.execute {
        indexInto(index).source(item).id(item.id)
      }
    }
    blockUntilExactCount(101, index.name)

    // Sanity test
    search(index).limit(200) should containId("nid1-xyo")
    search(index).limit(200) should containId("nid101-xyz")
    search(index).termQuery("notificationType", "HeronDefeat") should haveTotalHits(50)

    // The IDs of notifications we expect our recipient to get.
    lazy val recipientNotifications: IndexedSeq[IndexedNotification] = items.filter(_.recipient == recipient)
    lazy val expectedIds: IndexedSeq[String] = recipientNotifications.map(_.notification.id)
    lazy val criticalIds: IndexedSeq[String] = recipientNotifications.filter(_.notification.priority == Critical).map(_.notification.id)
    lazy val warningIds: IndexedSeq[String] =
      recipientNotifications.filter { i => i.notification.priority == Warning || i.notification.priority == Critical }
        .map(_.notification.id)
  }

  @Before def setUp(): Unit = {
    new NotificationElasticsearchConfig {
      client.execute {
        createIndex(index.name).mapping(properties(fields)).analysis(analysis)
      }.await.result.acknowledged should be(true)
    }
    blockUntilIndexExists(index.name)
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
    blockUntilIndexNotExists(index.name)
  }

  @Test def ignoreDismissed(): Unit = new IndexedDataFixture {
    val request = ActivityStreamRequest(user = recipient, max = 100, lastUpdatedDate = None)
    queryService.userStream(request).items.size should be(50)

    val includeDismissed = ActivityStreamRequest(user = recipient, includeDismissed = true, max = 100, lastUpdatedDate = None)
    queryService.userStream(includeDismissed).items.size should be(51)
  }

  @Test
  def userStream(): Unit = new IndexedDataFixture {
    val request = ActivityStreamRequest(user = recipient, max = 20, lastUpdatedDate = None)
    val page1: PagedNotifications = queryService.userStream(request)
    page1.items.size should be(20)

    page1.items.map(_.id) should be(expectedIds.reverse.slice(0, 20))

    val page2: PagedNotifications = queryService.userStream(request.copy(lastUpdatedDate = page1.lastUpdatedDate))
    page2.items.size should be(20)

    page2.items.map(_.id) should be(expectedIds.reverse.slice(20, 40))
  }

  @Test
  def typeFilteredUserStreamEmpty(): Unit = new IndexedDataFixture {
    val request = ActivityStreamRequest(user = recipient, types = Some(Set("Nonexistent")), lastUpdatedDate = None)
    queryService.userStream(request).items.size should be(0)
  }

  @Test
  def typeFilteredUserStream(): Unit = new IndexedDataFixture {
    val request = ActivityStreamRequest(user = otherRecipient, types = Some(Set("HeronDefeat")), lastUpdatedDate = None)
    queryService.userStream(request).items.size should be(50)
  }

  @Test
  def priorityFilteredUserStream(): Unit = new IndexedDataFixture {
    // show critical items only - should be 10 items
    val criticalRequest = ActivityStreamRequest(user = recipient, priority = 0.75, max = 20, lastUpdatedDate = None)
    val page: PagedNotifications = queryService.userStream(criticalRequest)
    page.items.size should be(10)
    page.items.map(_.id) should be(criticalIds.reverse)

    // show >= warning items only - should be 30 items
    val warningRequest = ActivityStreamRequest(user = recipient, priority = 0.5, max = 20, lastUpdatedDate = None)
    val page1: PagedNotifications = queryService.userStream(warningRequest)
    page1.items.size should be(20)
    page1.items.map(_.id) should be(warningIds.reverse.slice(0, 20))

    val page2: PagedNotifications = queryService.userStream(warningRequest.copy(lastUpdatedDate = page1.lastUpdatedDate))
    page2.items.size should be(10)
    page2.items.map(_.id) should be(warningIds.reverse.slice(20, 30))
  }

}

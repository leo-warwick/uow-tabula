package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.testkit.{SearchMatchers, IndexMatchers, ElasticSugar}
import org.joda.time.DateTime
import org.junit.{After, Before}
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ActivityStreamRequest
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class NotificationQueryServiceTest extends TestBase with Mockito with ElasticSugar with IndexMatchers with SearchMatchers {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	val indexName = "notifications"

	private trait Fixture {
		val queryService = new NotificationQueryServiceImpl
		queryService.notificationDao = smartMock[NotificationDao]
		queryService.client = NotificationQueryServiceTest.this.client
		queryService.indexName = NotificationQueryServiceTest.this.indexName

		// default behaviour, we add individual expectations later
//		queryService.notificationDao.getById(any[String]) returns None

		implicit val indexable = NotificationIndexService.IndexedNotificationIndexable
	}

	private trait IndexedDataFixture extends Fixture {
		val agent = Fixtures.user(userId="abc")
		val recipient = Fixtures.user(userId="xyz")
		val otherRecipient = Fixtures.user(userId="xyo")

		val now = DateTime.now

		// Selection of notifications intended for a couple of different recipients
		lazy val items = for (i <- 1 to 100) yield {
			val notification =
				if (i % 2 == 0) {
					new HeronWarningNotification
				} else {
					new HeronDefeatedNotification
				}
			notification.id = "nid"+i
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

		lazy val dismissedItem = {
			val heron2 = new Heron(recipient)
			val notification = Notification.init(new HeronWarningNotification, agent, heron2)
			notification.id = "nid101"
			notification.created = now.plusMinutes(101)
			notification.dismiss(recipient)
			IndexedNotification(notification, recipient)
		}

		(items :+ dismissedItem).foreach { item =>
			queryService.notificationDao.getById(item.notification.id) returns Some(item.notification)
			client.execute { index into indexName -> indexName source item id item.id }
		}
		blockUntilExactCount(101, indexName, indexName)

		// Sanity test
		search in indexName / indexName limit 200 should containResult("nid1-xyo")
		search in indexName / indexName limit 200 should containResult("nid101-xyz")
		search in indexName / indexName term("notificationType", "HeronDefeat") should haveTotalHits(50)

		// The IDs of notifications we expect our recipient to get.
		lazy val recipientNotifications = items.filter { _.recipient == recipient }
		lazy val expectedIds = recipientNotifications.map { _.notification.id }
		lazy val criticalIds = recipientNotifications.filter { _.notification.priority == Critical }.map { _.notification.id }
		lazy val warningIds =
			recipientNotifications.filter {i => i.notification.priority == Warning || i.notification.priority == Critical}
				.map { _.notification.id }
	}

	@Before def setUp(): Unit = {
		new NotificationElasticsearchConfig {
			client.execute {
				create index indexName mappings (mapping(indexName) fields fields) analysis analysers
			}.await.isAcknowledged should be(true)
		}
		blockUntilIndexExists(indexName)
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }
		blockUntilIndexNotExists(indexName)
	}

	@Test def ignoreDismissed(): Unit = new IndexedDataFixture {
		val request = ActivityStreamRequest(user = recipient, max = 100, lastUpdatedDate = None)
		queryService.userStream(request).futureValue.items.size should be (50)

		val includeDismissed = ActivityStreamRequest(user = recipient, includeDismissed = true, max = 100, lastUpdatedDate = None)
		queryService.userStream(includeDismissed).futureValue.items.size should be (51)
	}

	@Test
	def userStream(): Unit = new IndexedDataFixture {
		val request = ActivityStreamRequest(user = recipient, max = 20, lastUpdatedDate = None)
		val page1 = queryService.userStream(request).futureValue
		page1.items.size should be (20)

		page1.items.map { _.id } should be (expectedIds.reverse.slice(0, 20))

		val page2 = queryService.userStream(request.copy(lastUpdatedDate = page1.lastUpdatedDate)).futureValue
		page2.items.size should be (20)

		page2.items.map { _.id } should be (expectedIds.reverse.slice(20, 40))
	}

	@Test
	def typeFilteredUserStreamEmpty(): Unit = new IndexedDataFixture {
		val request = ActivityStreamRequest(user = recipient, types = Some(Set("Nonexistent")), lastUpdatedDate = None)
		queryService.userStream(request).futureValue.items.size should be (0)
	}

	@Test
	def typeFilteredUserStream(): Unit = new IndexedDataFixture {
		val request = ActivityStreamRequest(user = otherRecipient, types = Some(Set("HeronDefeat")), lastUpdatedDate = None)
		queryService.userStream(request).futureValue.items.size should be (50)
	}

	@Test
	def priorityFilteredUserStream(): Unit = new IndexedDataFixture {
		// show critical items only - should be 10 items
		val criticalRequest = ActivityStreamRequest(user = recipient, priority = 0.75, max = 20, lastUpdatedDate = None)
		val page = queryService.userStream(criticalRequest).futureValue
		page.items.size should be (10)
		page.items.map { _.id } should be (criticalIds.reverse)

		// show >= warning items only - should be 30 items
		val warningRequest = ActivityStreamRequest(user = recipient, priority = 0.5, max = 20, lastUpdatedDate = None)
		val page1 = queryService.userStream(warningRequest).futureValue
		page1.items.size should be (20)
		page1.items.map { _.id } should be (warningIds.reverse.slice(0, 20))

		val page2 = queryService.userStream(warningRequest.copy(lastUpdatedDate = page1.lastUpdatedDate)).futureValue
		page2.items.size should be (10)
		page2.items.map { _.id } should be (warningIds.reverse.slice(20, 30))
	}

}

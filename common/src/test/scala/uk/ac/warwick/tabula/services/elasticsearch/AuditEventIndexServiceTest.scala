package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{RichGetResponse, RichSearchResponse}
import com.sksamuel.elastic4s.testkit.IndexMatchers
import org.elasticsearch.search.sort.SortOrder
import org.hibernate.Session
import org.hibernate.dialect.HSQLDialect
import org.joda.time.DateTime
import org.junit.After
import org.scalatest.time.{Millis, Seconds, Span}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.AuditEventServiceImpl
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase, TestElasticsearchClient}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.StopWatch

import scala.collection.JavaConverters._
import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

class AuditEventIndexServiceTest extends PersistenceTestBase with Mockito with TestElasticsearchClient with IndexMatchers {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	val indexName = "audit"
	val indexType: String = new AuditEventIndexType {}.indexType

	private trait Fixture {
		val service: AuditEventServiceImpl = new AuditEventServiceImpl with SessionComponent {
			val session: Session = sessionFactory.getCurrentSession
		}
		service.dialect = new HSQLDialect()

		val indexer = new AuditEventIndexService
		indexer.indexName = AuditEventIndexServiceTest.this.indexName
		indexer.client = AuditEventIndexServiceTest.this.client
		indexer.auditEventService = service
		service.auditEventIndexService = indexer

		// Creates the index
		indexer.ensureIndexExists().await should be (true)

		implicit val indexable: ElasticsearchIndexable[AuditEvent] = AuditEventIndexService.auditEventIndexable(service)
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }.await
		session.createSQLQuery("delete from auditevent").executeUpdate()
	}

	@Transactional
	@Test def fields(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val event = AuditEvent(
			eventId="eventId", eventType="MyEventType", userId="cuscav", eventDate=DateTime.now(),
			eventStage="after", data="""{"assignment":"12345"}"""
		)
		event.related = Seq(event)

		indexer.indexItems(Seq(event)).await
		blockUntilCount(1, indexName, indexType)

		// University ID is the ID field so it isn't in the doc source
		val doc: RichGetResponse = client.execute { get id event.id from indexName / indexType }.futureValue

		doc.source.asScala.toMap should be (Map(
			"eventId" -> "eventId",
			"eventType" -> "MyEventType",
			"eventDate" -> "2000-06-01T00:00:00+01:00",
			"userId" -> "cuscav",
			"assignment" -> "12345"
		))
	}}

	@Transactional
	@Test def index(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val stopwatch = new StopWatch

		val jsonData = Map(
			"students" -> Array("jeb", "joe")
		)
		val jsonDataString: String = json.writeValueAsString(jsonData)

		stopwatch.start("creating items")

		val defendEvents: IndexedSeq[AuditEvent] = for (i <- 1 to 1000)
			yield AuditEvent(
				eventId="d"+i, eventType="DefendBase", eventStage="before", userId="jim",
				eventDate=new DateTime(2000,1,2,0,0,0).plusSeconds(i),
				data="{}"
			)

		val publishEvents: IndexedSeq[AuditEvent] = for (i <- 1 to 20)
			yield AuditEvent(
				eventId="s"+i, eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(i),
				data=jsonDataString
			)

		stopwatch.stop()

		def addParsedData(event: AuditEvent): AuditEvent = {
			event.parsedData = service.parseData(event.data)
			event
		}

		val events: IndexedSeq[AuditEvent] = defendEvents ++ publishEvents

		// Do this 50 at a time to avoid saturating the internal Elasticsearch server's bulk indexing threadpool
		events.grouped(50).zipWithIndex.foreach { case (e, groupNum) =>
			e.foreach { event => service.save(addParsedData(event)) }
			blockUntilCount((groupNum * 50) + e.size, indexName, indexType)
		}

		service.listNewerThan(new DateTime(2000,1,1,0,0,0), 100).size should be (100)

		// Should have indexed as part of the save process

		blockUntilCount(1020, indexName, indexType)
		client.execute { search in indexName / indexType }.await.totalHits should be (1020)

		val user = new User("jeb")
		user.setWarwickId("0123456")

		def listRecent(max: Int): Future[RichSearchResponse] =
			client.execute { search in indexName / indexType sort ( field sort "eventDate" order SortOrder.DESC ) limit max }

		def resultsForStudent(user: User): Future[RichSearchResponse] =
			client.execute { search in indexName / indexType query termQuery("students", user.getUserId) }

		listRecent(1000).futureValue.hits.length should be (1000)

		resultsForStudent(user).futureValue.totalHits should be (20)

		val moreEvents: Seq[AuditEvent] = {
			val events = Seq(addParsedData(AuditEvent(
				eventId="x9000", eventType="PublishFeedback", eventStage="before", userId="bob",
				eventDate=new DateTime(2000,1,1,0,0,0).plusSeconds(9000),
				data=jsonDataString
			)))
			events.foreach(service.save)
			service.getByEventId("x9000")
		}
		indexer.indexItems(moreEvents)

		blockUntilCount(1021, indexName, indexType)

		resultsForStudent(user).futureValue.totalHits should be (21)

		listRecent(13).futureValue.hits.length should be (13)

		val publishFeedback: Future[RichSearchResponse] =
			client.execute { search in indexName / indexType query queryStringQuery("eventType:PublishFeedback") limit 100 }

		publishFeedback.futureValue.totalHits should be (21)

		// First query is slowest, but subsequent queries quickly drop
		// to a much smaller time
		for (i <- 1 to 20) {
			stopwatch.start("searching for newest item forever attempt " + i)
			val newest =
				client.execute { search in indexName / indexType sort ( field sort "eventDate" order SortOrder.DESC ) limit 1 }

			newest.futureValue.hits.head.sourceAsMap("eventId") should be ("d1000")
			stopwatch.stop()
		}

		// index again to check that it doesn't do any once-only stuff
		indexer.indexFrom(indexer.newestItemInIndexDate.await.get).await
	}}

}

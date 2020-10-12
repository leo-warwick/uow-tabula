package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.testkit.IndexMatchers
import com.sksamuel.elastic4s.{Index, Response}
import org.hibernate.Session
import org.joda.time.DateTime
import org.junit.After
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.AuditEventServiceImpl
import uk.ac.warwick.tabula.system.PostgreSQL10Dialect
import uk.ac.warwick.tabula.{DateFormats, Mockito, PersistenceTestBase, TestElasticsearchClient}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.StopWatch

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

class AuditEventIndexServiceTest extends PersistenceTestBase with Mockito with TestElasticsearchClient with IndexMatchers {

  val index = Index("audit")

  private trait Fixture {
    val service: AuditEventServiceImpl = new AuditEventServiceImpl with SessionComponent {
      val session: Session = sessionFactory.getCurrentSession
    }
    service.dialect = new PostgreSQL10Dialect

    val indexer = new AuditEventIndexService
    indexer.indexName = AuditEventIndexServiceTest.this.index.name
    indexer.client = AuditEventIndexServiceTest.this.client
    indexer.auditEventService = service
    service.auditEventIndexService = indexer

    // Creates the index
    indexer.ensureIndexExists().await should be(true)

    implicit val indexable: ElasticsearchIndexable[AuditEvent] = AuditEventIndexService.auditEventIndexable(service)
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
    session.createSQLQuery("delete from auditevent").executeUpdate()
  }

  @Transactional
  @Test def fields(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val event = AuditEvent(
        eventId = "eventId", eventType = "MyEventType", userId = "cuscav", eventDate = DateTime.now(),
        eventStage = "after", data ="""{"assignment":"12345"}"""
      )
      event.related = Seq(event)

      indexer.indexItems(Seq(event)).await
      blockUntilCount(1, index.name)

      // University ID is the ID field so it isn't in the doc source
      val doc: GetResponse = client.execute {
        get(index, event.id.toString)
      }.futureValue.result

      doc.source should be(Map(
        "eventId" -> "eventId",
        "eventType" -> "MyEventType",
        "eventDate" -> DateFormats.IsoDateTime.print(DateTime.now),
        "userId" -> "cuscav",
        "assignment" -> "12345"
      ))
    }
  }

  @Transactional
  @Test def indexing(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val stopwatch = new StopWatch

      val jsonData: Map[String, Array[String]] = Map(
        "students" -> Array("jeb", "joe")
      )
      val jsonDataString: String = json.writeValueAsString(jsonData)

      stopwatch.start("creating items")

      val defendEvents: IndexedSeq[AuditEvent] = for (i <- 1 to 1000)
        yield AuditEvent(
          eventId = "d" + i, eventType = "DefendBase", eventStage = "before", userId = "jim",
          eventDate = new DateTime(2000, 1, 2, 0, 0, 0).plusSeconds(i),
          data = "{}"
        )

      val publishEvents: IndexedSeq[AuditEvent] = for (i <- 1 to 20)
        yield AuditEvent(
          eventId = "s" + i, eventType = "PublishFeedback", eventStage = "before", userId = "bob",
          eventDate = new DateTime(2000, 1, 1, 0, 0, 0).plusSeconds(i),
          data = jsonDataString
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
        blockUntilCount((groupNum * 50) + e.size, index.name)
      }

      service.listNewerThan(new DateTime(2000, 1, 1, 0, 0, 0), 100).size should be(100)

      // Should have indexed as part of the save process

      blockUntilCount(1020, index.name)
      client.execute {
        search(index)
      }.await.result.totalHits should be(1020)

      val user = new User("jeb")
      user.setWarwickId("0123456")

      def listRecent(max: Int): Future[Response[SearchResponse]] =
        client.execute {
          search(index).sortBy(fieldSort("eventDate").order(SortOrder.Desc)).limit(max)
        }

      def resultsForStudent(user: User): Future[Response[SearchResponse]] =
        client.execute {
          search(index) query termQuery("students", user.getUserId)
        }

      listRecent(1000).futureValue.result.hits.hits.length should be(1000)

      resultsForStudent(user).futureValue.result.totalHits should be(20)

      val moreEvents: Seq[AuditEvent] = {
        val events = Seq(addParsedData(AuditEvent(
          eventId = "x9000", eventType = "PublishFeedback", eventStage = "before", userId = "bob",
          eventDate = new DateTime(2000, 1, 1, 0, 0, 0).plusSeconds(9000),
          data = jsonDataString
        )))
        events.foreach(service.save)
        service.getByEventId("x9000")
      }
      indexer.indexItems(moreEvents)

      blockUntilCount(1021, index.name)

      resultsForStudent(user).futureValue.result.totalHits should be(21)

      listRecent(13).futureValue.result.hits.hits.length should be(13)

      val publishFeedback: Future[Response[SearchResponse]] =
        client.execute {
          search(index).query(queryStringQuery("eventType:PublishFeedback")).limit(100)
        }

      publishFeedback.futureValue.result.totalHits should be(21)

      // First query is slowest, but subsequent queries quickly drop
      // to a much smaller time
      for (i <- 1 to 20) {
        stopwatch.start("searching for newest item forever attempt " + i)
        val newest =
          client.execute {
            search(index).sortBy(fieldSort("eventDate").order(SortOrder.Desc)).limit(1)
          }

        newest.futureValue.result.hits.hits.head.sourceAsMap("eventId") should be("d1000")
        stopwatch.stop()
      }

      // index again to check that it doesn't do any once-only stuff
      indexer.indexFrom(indexer.newestItemInIndexDate.await.get).await
    }
  }

}

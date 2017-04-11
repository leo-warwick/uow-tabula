package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, RichSearchResponse}
import com.sksamuel.elastic4s.analyzers.SimpleAnalyzer
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.junit.{After, Before}
import uk.ac.warwick.tabula.data.model.Identifiable
import uk.ac.warwick.tabula.{DateFormats, ElasticsearchTestBase}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.util.Random

class ElasticsearchIndexingTest extends ElasticsearchTestBase {

	case class Item(id: Int, name: String, date: DateTime) extends Identifiable

	implicit object IndexableItem extends ElasticsearchIndexable[Item] {
		override def fields(item: Item): Map[String, Any] = Map(
			"name" -> item.name,
			"date" -> DateFormats.IsoDateTime.print(item.date)
		)

		override def lastUpdatedDate(item: Item): DateTime = item.date
	}

	val indexName = "mock"
	val indexType = "wibble"

	private trait ElasticsearchIndexingSupport extends ElasticsearchClientComponent {
		override val client: ElasticClient = ElasticsearchIndexingTest.this.client
	}

	private trait Fixture {
		val fakeItems: IndexedSeq[Item] = for (i <- 1 to 100) yield Item(i, s"item$i", new DateTime().plusMinutes(i))

		val service = new ElasticsearchIndexing[Item] with ElasticsearchIndexName with ElasticsearchIndexType with ElasticsearchIndexingSupport with ElasticsearchIndexEnsure {
			override val indexName: String = ElasticsearchIndexingTest.this.indexName
			override val indexType: String = ElasticsearchIndexingTest.this.indexType
			override val UpdatedDateField = "date"
			override val IncrementalBatchSize: Int = 1000
			override implicit val indexable = IndexableItem

			override protected def listNewerThan(startDate: DateTime, batchSize: Int): TraversableOnce[Item] = ???
			override def ensureIndexExists(): Future[Boolean] = Future.successful(true) // we do this in setup()
		}
	}

	@Before def setup(): Unit = {
		client.execute { create index indexName mappings (
			mapping(indexType) fields (
				stringField("name") analyzer SimpleAnalyzer,
				dateField("date") format "strict_date_time_no_millis"
			)
		) }.await.isAcknowledged should be (true)
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }.await
	}

	@Test def indexItems(): Unit = new Fixture {
		// Index at random to ensure things don't depend on ordered insertion
		val result: ElasticsearchIndexingResult = service.indexItems(Random.shuffle(fakeItems)).await

		result.successful should be (100)
		result.failed should be (0)

		// We block because ElasticSearch accepts items but they may not be returned until a refresh happens internally
		blockUntilCount(100, indexName, indexType)

		// Get the most recent item from a sort
		val searchResponse: RichSearchResponse =
			client.execute { search in indexName / indexType sort ( field sort "date" order SortOrder.DESC ) limit 1 }.await

		searchResponse.hits.length should be (1)
		searchResponse.totalHits should be (100)
		searchResponse.hits(0).id should be ("100")
		searchResponse.hits(0).sourceAsMap("name") should be ("item100")
		Option(searchResponse.hits(0).sourceAsMap("date")) should be ('defined)

		service.newestItemInIndexDate.futureValue should be (Some(fakeItems.last.date.withMillisOfSecond(0))) // millis are lost in indexing
	}

}

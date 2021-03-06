package uk.ac.warwick.tabula

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.indexes.admin.RefreshIndexResponse
import com.sksamuel.elastic4s.testkit.{ClientProvider, IndexMatchers, SearchMatchers}
import com.sksamuel.elastic4s._
import org.junit.{After, Before}
import org.scalatest.time.{Millis, Seconds, Span}
import org.testcontainers.elasticsearch.ElasticsearchContainer
import uk.ac.warwick.tabula.helpers.Logging

import scala.util.Try

abstract class ElasticsearchTestBase
  extends TestBase
    with TestElasticsearchClient
    with IndexMatchers
    with SearchMatchers {
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(200, Millis))
}

trait TestElasticsearchClient extends ElasticSugar with ClientProvider {
  self: TestHelpers with Logging =>

  private lazy val elastic =
    new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.9.2")
      .withEnv("cluster.name", "tabula-unit-tests")

  private var localClient: ElasticClient = _

  @Before def createClient(): Unit = {
    elastic.start()
    localClient =
      ElasticClient(JavaClient(ElasticProperties(s"http://${elastic.getHttpHostAddress}")))
  }

  @After def closeClient(): Unit = {
    localClient.close()
    elastic.stop()
  }

  override implicit def client: ElasticClient = localClient

}

// Copied from elastic4s but doesn't unnecessary extend ElasticDsl (which brings in its own logging)
trait ElasticSugar {
  self: ClientProvider with Logging =>

  // refresh all indexes
  def refreshAll(): RefreshIndexResponse = refresh(Indexes.All)

  // refreshes all specified indexes
  def refresh(indexes: Indexes): RefreshIndexResponse =
    client
      .execute {
        refreshIndex(indexes)
      }
      .await
      .result

  def blockUntilGreen(): Unit =
    blockUntil("Expected cluster to have green status") { () =>
      client
        .execute {
          clusterHealth()
        }
        .await
        .result
        .status
        .toUpperCase == "GREEN"
    }

  def blockUntil(explain: String)(predicate: () => Boolean): Unit = {

    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(200 * backoff)
      backoff = backoff + 1
      try done = predicate()
      catch {
        case e: Throwable =>
          logger.warn("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting on: $explain")
  }

  def ensureIndexExists(index: String): Unit =
    if (!doesIndexExists(index))
      client.execute {
        createIndex(index)
      }.await

  def doesIndexExists(name: String): Boolean =
    client
      .execute {
        indexExists(name)
      }
      .await
      .result
      .isExists

  def deleteIndex(name: String): Unit =
    Try {
      client.execute {
        ElasticDsl.deleteIndex(name)
      }.await
    }

  def truncateIndex(index: String): Unit = {
    deleteIndex(index)
    ensureIndexExists(index)
    blockUntilEmpty(index)
  }

  def blockUntilDocumentExists(id: String, index: String): Unit =
    blockUntil(s"Expected to find document $id") { () =>
      client
        .execute {
          get(index, id)
        }
        .await
        .result
        .exists
    }

  def blockUntilCount(expected: Long, index: String): Unit =
    blockUntil(s"Expected count of $expected") { () =>
      val result = client
        .execute {
          search(index).matchAllQuery().size(0)
        }
        .await
        .result
      expected <= result.totalHits
    }

  def blockUntilCount(expected: Long, index: Index): Unit = blockUntilCount(expected, index.name)

  def blockUntilExactCount(expected: Long, index: String): Unit =
    blockUntil(s"Expected count of $expected") { () =>
      expected == client
        .execute {
          search(index).size(0)
        }
        .await
        .result
        .totalHits
    }

  def blockUntilEmpty(index: String): Unit =
    blockUntil(s"Expected empty index $index") { () =>
      client
        .execute {
          search(Indexes(index)).size(0)
        }
        .await
        .result
        .totalHits == 0
    }

  def blockUntilIndexExists(index: String): Unit =
    blockUntil(s"Expected exists index $index") { () =>
      doesIndexExists(index)
    }

  def blockUntilIndexNotExists(index: String): Unit =
    blockUntil(s"Expected not exists index $index") { () =>
      !doesIndexExists(index)
    }

  def blockUntilDocumentHasVersion(index: String, id: String, version: Long): Unit =
    blockUntil(s"Expected document $id to have version $version") { () =>
      client
        .execute {
          get(index, id)
        }
        .await
        .result
        .version == version
    }
}

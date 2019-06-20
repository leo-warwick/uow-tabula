package uk.ac.warwick.tabula.services.elasticsearch

import java.io.Closeable

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.analyzers.AnalyzerDefinition
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldDefinition
import com.sksamuel.elastic4s.searches.sort.SortOrder
import com.sksamuel.elastic4s.{Index, IndexAndType, Indexable}
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Identifiable
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{DateFormats, JsonObjectMapperFactory}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

abstract class AbstractIndexService[A <: Identifiable]
  extends ElasticsearchClientComponent
    with ElasticsearchIndexName
    with ElasticsearchIndexType
    with ElasticsearchIndexInitialisation
    with ElasticsearchConfig
    with ElasticsearchIndexing[A] {

  @Autowired var client: ElasticClient = _

}

trait ElasticsearchIndexName {
  /**
    * The name of the index that this service writes to
    */
  def index: Index
}

trait ElasticsearchIndexType {
  /**
    * The object type that this service writes
    */
  def indexType: String
}

trait ElasticsearchIndexable[A] extends Indexable[A] {
  var json: ObjectMapper = JsonObjectMapperFactory.instance

  def fields(item: A): Map[String, Any]

  def lastUpdatedDate(item: A): DateTime

  override final def json(item: A): String = json.writeValueAsString(fields(item))
}

trait ElasticsearchIndexEnsure {
  def ensureIndexExists(): Future[Boolean]
}

trait ElasticsearchIndexInitialisation extends ElasticsearchIndexEnsure {
  self: ElasticsearchClientComponent
    with ElasticsearchIndexName
    with ElasticsearchIndexType
    with ElasticsearchConfig =>

  def ensureIndexExists(): Future[Boolean] = {
    // Initialise the index if it doesn't already exist
    def exists() = client.execute {
      indexExists(index.name)
    }.map(_.result.isExists)

    def aliasExists() = client.execute {
      indexExists(s"${index.name}-alias")
    }.map(_.result.isExists)

    def create() = client.execute {
      createIndex(index.name)
        .mappings(mapping(indexType).fields(fields))
        .analysis(analysers)
    }.map(_.result.acknowledged)

    def createAlias() = client.execute {
      addAlias(s"${index.name}-alias").on(index.name)
    }.map(_.result.acknowledged)

    exists().flatMap {
      case true => aliasExists().flatMap {
        case true => Future.successful(true)
        case false => createAlias()
      }
      case false =>
        create().filter { b => b }
          .flatMap { _ => createAlias() }
    }.filter { existsOrCreated => existsOrCreated } // throw an exception if it didn't work
  }
}

trait ElasticsearchConfig {
  def analysers: Seq[AnalyzerDefinition]

  def fields: Seq[FieldDefinition]
}

object ElasticsearchIndexingResult {
  def empty = ElasticsearchIndexingResult(0, 0, 0.millis, None)
}

case class ElasticsearchIndexingResult(successful: Int, failed: Int, timeTaken: Duration, maxUpdatedDate: Option[DateTime]) {
  def +(other: ElasticsearchIndexingResult) =
    ElasticsearchIndexingResult(successful + other.successful, failed + other.failed, timeTaken + other.timeTaken, (maxUpdatedDate.toSeq ++ other.maxUpdatedDate.toSeq).sorted.lastOption)
}

trait ElasticsearchIndexing[A <: Identifiable] extends Logging {
  self: ElasticsearchIndexEnsure
    with ElasticsearchClientComponent
    with ElasticsearchIndexName
    with ElasticsearchIndexType =>

  implicit val indexable: ElasticsearchIndexable[A]

  val UpdatedDateField: String

  // largest batch of items we'll load in at once during scheduled incremental index.
  val IncrementalBatchSize: Int

  @Value("${tabula.yearZero}") var yearZero: Int = 2000

  /**
    * Indexes a specific given list of items.
    */
  def indexItems(items: TraversableOnce[A]): Future[ElasticsearchIndexingResult] = {
    doIndexItems(items)
  }

  def indexItemsWithoutNewTransaction(items: TraversableOnce[A]): Future[ElasticsearchIndexingResult] = {
    doIndexItems(items)
  }

  /**
    * Wrapper around the indexing code so that it is only running once.
    * If it's already running, the code is skipped.
    * We only try indexing once a minute so there's no need to bother about
    * tight race conditions here.
    */
  private def guardMultipleIndexes[T](work: => T): T = this.synchronized(work)

  def newestItemInIndexDate: Future[Option[DateTime]] = client.execute {
    search(index)
      .sourceInclude(UpdatedDateField)
      .sortBy(fieldSort(UpdatedDateField).order(SortOrder.Desc))
      .limit(1)
  }.map { response =>
    response.result.hits.hits.headOption.map { hit =>
      DateFormats.IsoDateTime.parseDateTime(hit.sourceAsMap(UpdatedDateField).toString)
    }
  }

  def indexFrom(startDate: DateTime): Future[ElasticsearchIndexingResult] = {
    guardMultipleIndexes {
      ensureIndexExists().flatMap { _ =>
        // Keep going until we run out

        /**
          * Index a batch of items and return the max date returned
          */
        def indexBatch(newerThan: DateTime, acc: ElasticsearchIndexingResult): Future[ElasticsearchIndexingResult] = {
          val itemsToIndex = listNewerThan(newerThan, IncrementalBatchSize)

          doIndexItems(itemsToIndex)
            .andThen { case _ => // basically what you'd expect a finally block to do
              itemsToIndex match {
                case c: Closeable => c.close()
                case _ =>
              }
            }
            .flatMap { result =>
              result.maxUpdatedDate match {
                case None => Future.successful(acc)
                /*
                 * FIXME This will fail if we have multiple events in the same second and they've appeared halfway through a batch
                 * but I'm struggling to find a way to do this where it doesn't include the same item again in the next batch.
                 */
                case Some(updatedDate) => indexBatch(updatedDate.plusSeconds(1), acc + result)
              }
            }
        }

        // Recursion, playa
        indexBatch(startDate, ElasticsearchIndexingResult.empty)
      }
    }
  }

  protected def doIndexItems(in: TraversableOnce[A]): Future[ElasticsearchIndexingResult] = {
    if (in.isEmpty) {
      Future.successful(ElasticsearchIndexingResult.empty)
    } else {
      logger.debug(s"Writing to the $index/$indexType index")

      // ID to item
      val items = in.map { i => i.id.toString -> i }.toMap
      val maxDate = items.values.map(indexable.lastUpdatedDate).max

      val upserts =
        items.map { case (id, item) =>
          update(id)
            .in(IndexAndType(index.name, indexType))
            .docAsUpsert(true)
            .doc(item)
        }

      val future =
        client.execute {
          bulk(upserts)
        }.map { response =>
          if (response.result.hasFailures) {
            response.result.failures.foreach { item =>
              logger.warn(s"Error indexing item: ${item.error}")
            }
          }

          ElasticsearchIndexingResult(response.result.successes.length, response.result.failures.length, response.result.took.millis, Some(maxDate))
        }

      if (logger.isDebugEnabled)
        future.onComplete {
          case Success(result) =>
            logger.debug(s"Indexed ${result.successful} items in ${result.timeTaken}")
          case _ =>
        }

      future
    }
  }

  protected def listNewerThan(startDate: DateTime, batchSize: Int): TraversableOnce[A]
}
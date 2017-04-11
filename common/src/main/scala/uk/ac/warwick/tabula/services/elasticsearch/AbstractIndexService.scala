package uk.ac.warwick.tabula.services.elasticsearch

import java.io.Closeable

import com.fasterxml.jackson.databind.ObjectMapper
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.AnalyzerDefinition
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import com.sksamuel.elastic4s.source.Indexable
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Identifiable
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Stopwatches._
import uk.ac.warwick.tabula.{DateFormats, JsonObjectMapperFactory}

import scala.concurrent.Future
import scala.concurrent.duration._

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
	def indexName: String
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
		def exists() = client.execute { indexExists(indexName) }.map(_.isExists)
		def aliasExists() = client.execute { indexExists(s"$indexName-alias") }.map(_.isExists)
		def create() = client.execute { createIndex(indexName) mappings(mapping(indexType) fields fields) analysis analysers }.map(_.isAcknowledged)
		def createAlias() = client.execute { add alias s"$indexName-alias" on indexName }.map(_.isAcknowledged)

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
	def fields: Seq[TypedFieldDefinition]
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
	def indexItems(items: TraversableOnce[A]): Future[ElasticsearchIndexingResult] = transactional(readOnly = true) {
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
		search in indexName / indexType sourceInclude UpdatedDateField sort ( field sort UpdatedDateField order SortOrder.DESC ) limit 1
	}.map { response =>
		response.hits.headOption.map { hit =>
			DateFormats.IsoDateTime.parseDateTime(hit.sourceAsMap(UpdatedDateField).toString)
		}
	}

	def indexFrom(startDate: DateTime): Future[ElasticsearchIndexingResult] = transactional(readOnly = true) {
		guardMultipleIndexes { ensureIndexExists().flatMap { _ =>
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
					.flatMap { result => result.maxUpdatedDate match {
						case None => Future.successful(acc)
						/*
						 * FIXME This will fail if we have multiple events in the same second and they've appeared halfway through a batch
						 * but I'm struggling to find a way to do this where it doesn't include the same item again in the next batch.
						 */
						case Some(updatedDate) => indexBatch(updatedDate.plusSeconds(1), acc + result)
					}}
			}

			// Recursion, playa
			indexBatch(startDate, ElasticsearchIndexingResult.empty)
		}
	}}

	protected def doIndexItems(in: TraversableOnce[A]): Future[ElasticsearchIndexingResult] = {
		if (in.isEmpty) {
			Future.successful(ElasticsearchIndexingResult.empty)
		} else {
			logger.debug(s"Writing to the $indexName/$indexType index")

			// ID to item
			val items = in.map { i => i.id -> i }.toMap
			val maxDate = items.values.map(indexable.lastUpdatedDate).max

			val upserts =
				items.map { case (id, item) =>
					update(id) in s"$indexName/$indexType" docAsUpsert true doc item
				}

			val future =
				client.execute {
					bulk(upserts)
				}.map { result =>
					if (result.hasFailures) {
						result.failures.foreach { item =>
							logger.warn("Error indexing item", item.failure.getCause)
						}
					}

					ElasticsearchIndexingResult(result.successes.length, result.failures.length, result.took, Some(maxDate))
				}

			if (logger.isDebugEnabled)
				future.onSuccess { case result => logger.debug(s"Indexed ${result.successful} items in ${result.timeTaken}") }

			future
		}
	}

	protected def listNewerThan(startDate: DateTime, batchSize: Int): TraversableOnce[A]
}
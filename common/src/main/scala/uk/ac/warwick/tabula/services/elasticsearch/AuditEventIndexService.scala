package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.{AnalyzerDefinition, WhitespaceAnalyzer}
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import org.elasticsearch.common.xcontent.XContentBuilder
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.{AuditEventService, AuditEventServiceComponent}

import scala.collection.mutable

object AuditEventIndexService {
	/**
		* We maintain a list of keys that we index that we guarantee will have a consistent format.
		*/
	private val keysToIndex: Seq[String] = Seq(
		"submission",
		"feedback",
		"assignment",
		"module",
		"department",
		"submissionIsNoteworthy",
		"students",
		"studentUsercodes",
		"attachments"
	)

	def auditEventIndexable(auditEventService: AuditEventService) = new ElasticsearchIndexable[AuditEvent] {
		override def fields(item: AuditEvent): Map[String, Any] = {
			if (item.related == null || item.related.isEmpty) {
				// Populate related item info so we can put it in the map
				auditEventService.addRelated(item)
			}

			var fields = mutable.Map[String, Any]()

			fields += (
				"eventType" -> item.eventType,
				"eventDate" -> DateFormats.IsoDateTime.print(item.eventDate)
			)

			if (item.eventId != null) { // null for old events
				fields += ("eventId" -> item.eventId)
			}
			if (item.userId != null) { // system-run actions have no user
				fields += ("userId" -> item.userId)
			}
			if (item.masqueradeUserId != null) {
				fields += ("masqueradeUserId" -> item.masqueradeUserId)
			}

			// add data from all stages of the event, before and after.
			item.related.flatMap { related => auditEventService.parseData(related.data) }
				.flatten
				.filter { case (key, _) => keysToIndex.contains(key) }
				.foreach { case (key, value) => fields += (key -> value) }

			fields.toMap
		}

		override def lastUpdatedDate(item: AuditEvent): DateTime = item.eventDate
	}
}

trait AuditEventIndexType extends ElasticsearchIndexType {
	final val indexType = "auditEvent"
}

@Service
class AuditEventIndexService
	extends AbstractIndexService[AuditEvent]
		with AuditEventServiceComponent
		with AuditEventElasticsearchConfig
		with AuditEventIndexType {

	override implicit lazy val indexable: ElasticsearchIndexable[AuditEvent] = AuditEventIndexService.auditEventIndexable(auditEventService)

	/**
		* The name of the index that this service writes to
		*/
	@Value("${elasticsearch.index.audit.name}") var indexName: String = _

	@Autowired var auditEventService: AuditEventService = _

	// largest batch of event items we'll load in at once during scheduled incremental index.
	final override val IncrementalBatchSize = 1000

	override val UpdatedDateField = "eventDate"

	override def listNewerThan(startDate: DateTime, batchSize: Int): Seq[AuditEvent] =
		auditEventService.listNewerThan(startDate, batchSize)
}

trait AuditEventElasticsearchConfig extends ElasticsearchConfig {
	override val fields: Seq[TypedFieldDefinition] = Seq(
		stringField("students") analyzer WhitespaceAnalyzer,
		stringField("feedbacks") analyzer WhitespaceAnalyzer,
		stringField("submissions") analyzer WhitespaceAnalyzer,
		stringField("attachments") analyzer WhitespaceAnalyzer,
		dateField("eventDate") format "strict_date_time_no_millis"
	)

	override val analysers: Seq[AnalyzerDefinition] = Seq(
		KeywordAnalyzerDefinition("default")
	)
}

case class KeywordAnalyzerDefinition(override val name: String) extends AnalyzerDefinition(name) {
	def build(source: XContentBuilder): Unit = {
		source.field("type", "keyword")
	}
}
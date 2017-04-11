package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.ElasticsearchTestBase

class ElasticsearchIndexInitialisationTest extends ElasticsearchTestBase {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	private trait ElasticsearchIndexSupport extends ElasticsearchClientComponent {
		override val client: ElasticClient = ElasticsearchIndexInitialisationTest.this.client
	}

	private trait Fixture {
		val indexName = "mock"
		val indexType = "wibble"

		val service = new ElasticsearchIndexInitialisation with ElasticsearchIndexName with ElasticsearchIndexType with ElasticsearchIndexSupport with AuditEventElasticsearchConfig {
			override val indexName: String = Fixture.this.indexName
			override val indexType: String = Fixture.this.indexType
		}
	}

	@Test
	def indexCreatedOnPropertiesSet(): Unit = new Fixture {
		indexName should not(beCreated)

		service.ensureIndexExists().futureValue should be (true)
		indexName should beCreated

		// Ensure that future runs of afterPropertiesSet don't affect this
		service.ensureIndexExists().futureValue should be (true)
		indexName should beCreated
	}

}

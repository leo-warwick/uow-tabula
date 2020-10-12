package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, Index}
import uk.ac.warwick.tabula.ElasticsearchTestBase

class ElasticsearchIndexInitialisationTest extends ElasticsearchTestBase {

  private trait ElasticsearchIndexSupport extends ElasticsearchClientComponent {
    override val client: ElasticClient = ElasticsearchIndexInitialisationTest.this.client
  }

  private trait Fixture {
    val index = Index("mock-index")
    val indexType = "wibble"

    val service = new ElasticsearchIndexInitialisation with ElasticsearchIndexName with ElasticsearchIndexSupport with AuditEventElasticsearchConfig {
      override val index: Index = Fixture.this.index
    }
  }

  @Test
  def indexCreatedOnPropertiesSet(): Unit = new Fixture {
    index.name should not(beCreated)

    service.ensureIndexExists().futureValue should be(true)
    index.name should beCreated

    // Ensure that future runs of afterPropertiesSet don't affect this
    service.ensureIndexExists().futureValue should be(true)
    index.name should beCreated
  }

}

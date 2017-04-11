package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, SearchDefinition}
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractQueryService
	extends ElasticsearchClientComponent
		with ElasticsearchIndexName
		with ElasticsearchIndexType
		with ElasticsearchSearching {

	@Autowired var client: ElasticClient = _

}

trait ElasticsearchSearching {
	self: ElasticsearchClientComponent
		with ElasticsearchIndexName
		with ElasticsearchIndexType =>

	protected def searchFor: SearchDefinition = search in indexName / indexType

}

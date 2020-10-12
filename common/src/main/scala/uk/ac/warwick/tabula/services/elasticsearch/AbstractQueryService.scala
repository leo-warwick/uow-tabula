package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractQueryService
  extends ElasticsearchClientComponent
    with ElasticsearchIndexName
    with ElasticsearchSearching {

  @Autowired var client: ElasticClient = _

}

trait ElasticsearchSearching {
  self: ElasticsearchClientComponent
    with ElasticsearchIndexName =>

  protected def searchRequest: SearchRequest = search(index)

}

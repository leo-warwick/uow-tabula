package uk.ac.warwick.tabula

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.testkit.{SearchMatchers, IndexMatchers, ElasticSugar}
import org.elasticsearch.common.settings.Settings
import org.junit.{After, Before}

abstract class ElasticsearchTestBase
	extends TestBase
		with TestElasticsearchClient
		with IndexMatchers
		with SearchMatchers

trait TestElasticsearchClient extends ElasticSugar {
	self: TestHelpers =>

	private var localClient: ElasticClient = _

	@Before def createClient(): Unit = {
		localClient = {
			val baseDir = createTemporaryDirectory()
			val confDir = createTemporaryDirectory()

			val settings = Settings.settingsBuilder()
				.put("node.http.enabled", httpEnabled)
				.put("http.enabled", httpEnabled)
				.put("path.home", baseDir.getAbsolutePath)
				.put("path.repo", baseDir.getAbsolutePath)
				.put("path.conf", confDir.getAbsolutePath)
				.put("index.number_of_shards", 3)
				.put("index.number_of_replicas", 0)
				.put("index.refresh_interval", "1s")
				.put("discovery.zen.ping.multicast.enabled", "false")
				.put("es.logger.level", "DEBUG")
				.put("cluster.name", getClass.getSimpleName)

			configureSettings(settings)

			ElasticClient.local(settings.build)
		}
	}

	@After def closeClient(): Unit = {
		localClient.close()
	}

	override implicit def client: ElasticClient = localClient

}

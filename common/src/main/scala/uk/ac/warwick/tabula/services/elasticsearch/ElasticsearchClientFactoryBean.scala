package uk.ac.warwick.tabula.services.elasticsearch

import java.io.File

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.common.settings.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class ElasticsearchClientFactoryBean extends ScalaFactoryBean[ElasticClient] {

	@Value("${elasticsearch.cluster.name}") var clusterName: String = _
	@Value("${elasticsearch.cluster.nodes}") var nodes: Array[String] = _
	@Value("${elasticsearch.cluster.detect_other_nodes}") var detectOtherNodes: Boolean = _

	// Should only really be used for unit tests
	@Value("${elasticsearch.cluster.local_jvm}") var localJvmCluster: Boolean = _
	@Value("${filesystem.index.dir}") var localIndexDirectory: File = _

	override def createInstance(): ElasticClient = {
		if (localJvmCluster) {
			localIndexDirectory.mkdirs()

			val conf = new File(localIndexDirectory, "config")
			conf.mkdirs()

			ElasticClient.local(
				Settings.settingsBuilder()
					.put("path.home", localIndexDirectory.getAbsolutePath)
					.put("path.repo", localIndexDirectory.getAbsolutePath)
					.put("path.conf", conf.getAbsolutePath)
					.put("node.http.enabled", false)
					.put("http.enabled", false)
					.put("index.number_of_shards", 1)
					.put("index.number_of_replicas", 0)
					.put("discovery.zen.ping.multicast.enabled", false)
					.put("script.groovy.indy", false)
					.put("cluster.name", clusterName)
				  .build()
			)
		} else {
			val settings =
				Settings.settingsBuilder()
					.put("cluster.name", clusterName)
					.put("client.transport.sniff", detectOtherNodes)
					.build()

			ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://${nodes.mkString(",")}"))
		}
	}

	override def destroyInstance(instance: ElasticClient): Unit = instance.close()
}

trait ElasticsearchClientComponent {
	def client: ElasticClient
}

trait AutowiringElasticsearchClientComponent extends ElasticsearchClientComponent {
	var client: ElasticClient = Wire[ElasticClient]
}

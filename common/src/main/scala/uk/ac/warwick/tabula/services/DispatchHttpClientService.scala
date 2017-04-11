package uk.ac.warwick.tabula.services

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{Http, thread}
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.params.HttpConnectionParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class DispatchHttpClientFactoryBean extends ScalaFactoryBean[Http] {

	@Value("${httpclient.connectTimeout:20000}") var connectTimeout: Int = _
	@Value("${httpclient.socketTimeout:20000}") var socketTimeout: Int = _

	override def createInstance(): Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, connectTimeout)
			HttpConnectionParams.setSoTimeout(getParams, socketTimeout)
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroyInstance(http: Http): Unit = http.shutdown()

}

trait DispatchHttpClientComponent {
	def httpClient: Http
}

trait AutowiringDispatchHttpClientComponent extends DispatchHttpClientComponent {
	var httpClient: Http = Wire[Http]
}
package uk.ac.warwick.tabula

import dispatch.classic.{Request, StatusCode, url, NoLogging, thread, Http}
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import scala.util.{Failure, Success, Try}
import java.io.InputStream

class FileDownload(uri: String, credentials: Option[LoginDetails]) {

	lazy val http: Http = new Http with thread.Safety with NoLogging {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	val request = (url(uri))
	val authorizedRequest = credentials map (cred => request.as_!(cred.usercode, cred.password)) getOrElse (request)

	def streamToString(is:InputStream):String = new String(Stream.continually(is.read).takeWhile(-1 != _).map(_.toByte).toArray,"UTF-8" )

	case class Result(content:Option[String], statusCode:Option[Int])

	lazy val result = {
		// as_str barfs with an EncodingException, on feedback PDFs even though everything renders fine.
		// manually reading the content into a UTF-8 string seems to work, so we'll stick with that for now.
		Try(http(authorizedRequest >>{is=>streamToString(is)})) match {
			case Success(src) => Result(Some(src), Some(200)) // treat things like 201, 204, as 200.
			case Failure(StatusCode(code, contents)) => Result(None, Some(code))
			case Failure(unexpectedException)=>throw unexpectedException
		}
	}

	def isSuccessful: Boolean = result.statusCode == Some(200)

	def contentAsString: String = result.content.getOrElse("")

	def as(credentials:LoginDetails) = new FileDownload(uri,Some(credentials))
}
object Download {

	def apply(url: String): FileDownload = {
		new FileDownload(url, None)
	}

}
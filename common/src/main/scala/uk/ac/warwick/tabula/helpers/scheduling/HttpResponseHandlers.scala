package uk.ac.warwick.tabula.helpers.scheduling

import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler

trait HttpResponseHandlers {
	def handle[A](fn: => (HttpResponse => A)) = new FunctionalResponseHandler[A](fn)

	def convert[A,B](pair: uk.ac.warwick.util.collections.Pair[A, B]): (A, B) = (pair.getLeft, pair.getRight)
}

class FunctionalResponseHandler[A](fn: => (HttpResponse => A)) extends ResponseHandler[A] {
	override def handleResponse(response: HttpResponse): A = fn(response)
}
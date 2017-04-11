package uk.ac.warwick.tabula.system

import org.springframework.http.MediaType
import org.springframework.web.accept.ContentNegotiationStrategy
import org.springframework.web.context.request.NativeWebRequest
import uk.ac.warwick.tabula.JavaImports._

class MultipleFixedContentNegotiationStrategy(mediaTypes: JList[MediaType]) extends ContentNegotiationStrategy {
	override def resolveMediaTypes(webRequest: NativeWebRequest): JList[MediaType] = mediaTypes
}

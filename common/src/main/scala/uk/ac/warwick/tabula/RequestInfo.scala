package uk.ac.warwick.tabula

import org.springframework.web.context.request.{RequestAttributes, RequestContextHolder, ServletRequestAttributes}
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.UriComponentsBuilder
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.util.web.Uri

/**
 * Stores information about the current request, such as the
 * current user.
 *
 * RequestInfo should be available even for scheduled jobs that
 * aren't directly part of an HTTP request so it should not expose
 * any Servlet specific stuff.
 *
 * It will be available anywhere from the thread but this should
 * not be used as an excuse to use it as a dumping ground for
 * "globals". Use dependency injection where possible. This is used
 * for things like the current user in situations like audit logging,
 * where it isn't appropriate to pass the user in to the method.
 */
class RequestInfo(
	val user: CurrentUser,
	val requestedUri: Uri,
	val requestParameters: Map[String, Seq[String]],
	val ajax: Boolean = false,
	val maintenance: Boolean = false,
	val requestLevelCache: RequestLevelCache = new RequestLevelCache,
	val hasEmergencyMessage: Boolean = false,
	val emergencyMessage: String = "",
	val userAgent: String = "",
	val ipAddress: String = ""
) extends EarlyRequestInfo

object RequestInfo {
	private val threadLocal = new ThreadLocal[Option[RequestInfo]] {
		override def initialValue = None
	}

	def fromThread: Option[RequestInfo] = threadLocal.get
	def open(info: RequestInfo): Unit = threadLocal.set(Some(info))

	def use[A](info: RequestInfo)(fn: => A): A =
		try { open(info); fn }
		finally close()

	def close() {
		fromThread foreach { _.requestLevelCache.shutdown() }
		threadLocal.remove()
	}

	def mappedPage: String = {
		// get the @RequestMapping (without path variables resolved), so that users don't get the same popup again
		// for a given kind of page with only variables changing
		val requestAttributes = RequestContextHolder.getRequestAttributes.asInstanceOf[ServletRequestAttributes]
		val mappedPage = requestAttributes.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST).toString

		mappedPage
	}
}


/**
 * A limited interface of things that can be available earlier on in a
 * request than the whole RequestInfo. This is specifically to work around
 * a cyclic dependency where creating a CurrentUser needs to use caching,
 * but the caching is in RequestInfo which needs the CurrentUser to be created.
 */
trait EarlyRequestInfo {
	val requestLevelCache: RequestLevelCache
}

object EarlyRequestInfo {
	private val threadLocal = new ThreadLocal[Option[EarlyRequestInfo]] {
		override def initialValue = None
	}

	def open(info: EarlyRequestInfo): Unit = threadLocal.set(Some(info))

	/** Only useful for an edge case. Use #fromThread usually.
		* If a full RequestInfo is available, this is used instead of
		* the EarlyRequestInfo. This is so tests can set up a RequestInfo
		* as normal and don't need updating.
		*/
	def fromThread: Option[EarlyRequestInfo] = RequestInfo.fromThread orElse threadLocal.get

	def close() {
		fromThread foreach { _.requestLevelCache.shutdown() }
		threadLocal.remove()
	}
}

class EarlyRequestInfoImpl extends EarlyRequestInfo {
	val requestLevelCache: RequestLevelCache = new RequestLevelCache()
}
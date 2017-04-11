package uk.ac.warwick.tabula.web.filters

import java.io._

import org.springframework.util.FileCopyUtils
import org.springframework.web.multipart.MultipartResolver
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.util.concurrency.TaskExecutionService

import scala.collection.JavaConverters._
import javax.servlet.{Filter, FilterChain, ReadListener, ServletInputStream}
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse}

import org.slf4j.{Logger, LoggerFactory}
import uk.ac.warwick.util.web.filter.AbstractHttpFilter
import uk.ac.warwick.tabula.helpers.{Logging, Runnable}
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Logs POST data to the POST_LOGGER category, which we append to a post.log file.
 *
 * Because a request body can only be read once, for a multipart request (which we need
 * to read to get the files) we wrap the input stream so that it pipes all read data
 * out to another buffered input stream, which an execution service then feeds to
 * the post logger. The main reader is completely unaware of this so it should work as normal.
 */
class PostDataLoggingFilter extends AbstractHttpFilter with Filter with Logging {

	import PostDataLoggingFilter._

	@transient lazy val postLogger: Logger = LoggerFactory.getLogger("POST_LOGGER")

	var multipartResolver: MultipartResolver = Wire[MultipartResolver]

	val executionService = new TaskExecutionService

	// For tests to access the Future of an asynchronous task
	val futureAttributeName = s"${getClass.getName}.future"

	def doFilter(
			request: HttpServletRequest,
			response: HttpServletResponse,
			chain: FilterChain): Unit = {

		var passthroughRequest = request

		// Only run on POST requests.
		// Also only run if not multipart - partly because getParameter doesn't support it,
		// but reading parameters also messes with file uploads.
		if (request.getMethod.equalsIgnoreCase("post")) {
			if (logger.isDebugEnabled()) {
				logger.debug("Logging POST data for request to " + request.getRequestURI)
			}

			if (multipartResolver.isMultipart(request) || isLogRequestBody(request)) {
				// Multipart - wrap the request to spy on the body as it's read,
				// without disturbing the original reader.
				val forkingRequest = new ForkingInputStreamHttpServletRequest(request)
				passthroughRequest = forkingRequest
				val future = executionService.submit(Runnable {
					postLogger.info(generateLogLine(forkingRequest.secondaryStreamRequestWrapper))
				})
				// Store the future in the request for tests
				forkingRequest.setAttribute(futureAttributeName, future)
			} else {
				// Non-multipart - generate the log line now
				postLogger.info(generateLogLine(request))
			}
		}

		// continue the chain
		chain.doFilter(passthroughRequest, response)
	}

	def generateLogLine(request: HttpServletRequest): String = {
		val data = new StringBuilder()
		val multipart = multipartResolver.isMultipart(request)

		data.append("userId=").append(SSOClientFilter.getUserFromRequest(request).getUserId)
		data.append(" ")
		data.append("multipart=").append(multipart)
		data.append(" ")
		data.append(request.getRequestURI)
		data.append(" ")

		val paramKeys = request.getParameterMap.keySet.asScala
		val allParams = paramKeys.flatMap { (key) =>
			request.getParameterValues(key).map { (value) =>
				s"$key=$value"
			}
		}.mkString("&")

		data.append(allParams)

		if (!multipart && isLogRequestBody(request)) {
			data.append("requestBody=")
			data.append(new String(FileCopyUtils.copyToByteArray(request.getInputStream), request.getCharacterEncoding))
		}

		data.toString
	}

	private def isLogRequestBody(request: HttpServletRequest) =
		request.getContentType.hasText &&
		request.getContentLength > 0 &&
		request.getContentLength < PostDataLoggingFilter.MaxRequestBodySizeToLog
}

object PostDataLoggingFilter {

	val MaxRequestBodySizeToLog: Int = 2 * 1024 * 1024 // 2mb

	/**
	 * This HttpServletRequestWrapper exposes an InputStream called `secondaryStream`,
	 * which will be piped the same data that is read from the request normally.
	 * This allows the usual request body reading to happen, while another thread
	 * (because it's a blocking stream) reads it for its own purposes. Buffering
	 * is handled by a PipedInputStream.
	 */
	class ForkingInputStreamHttpServletRequest(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {
		// Everything that gets read, is written into this pipe
		private val outputPipe = new PipedOutputStream()
		// Secondary is the other end of the pipe
		val secondaryStream = new PipedInputStream(outputPipe)
		// A request wrapper for the above stream to
		val secondaryStreamRequestWrapper = new SecondaryStreamRequestWrapper(this)

		override def getInputStream: FilterServletInputStream = {
			// Passes through reads as usual, but also writes any data
			// through PipedOutputStream to the PipedInputStream.
			new FilterServletInputStream(super.getInputStream) {

				override def read(): Int = {
					val b = super.read()
					if (b > -1) {
						outputPipe.write(b)
					}
					b
				}

				override def read(b: Array[Byte]): Int = read(b, 0, b.length)

				override def read(b: Array[Byte], off: Int, len: Int): Int = {
					val read = super.read(b, off, len)

					if (read > -1) {
						outputPipe.write(b, off, read)
					}

					read
				}

				override def close() {
					super.close()
					outputPipe.close()
				}
			}
		}
	}

	/**
	 * A wrapper of a wrapper!
	 * Because the Apache Commons expects an HttpServletRequest that it can call getInputStream on,
	 * we have this wrapper that returns the secondary stream instead of the main one.
	 */
	class SecondaryStreamRequestWrapper(request: ForkingInputStreamHttpServletRequest) extends HttpServletRequestWrapper(request) {
		override def getInputStream = new FilterServletInputStream(request.secondaryStream)
	}

	/**
	 * This is just like FilterInputStream, but for a ServletInputStream.
	 * Passes all calls to the delegate stream.
	 * They're both abstract classes so can't inherit both.
	 */
	class FilterServletInputStream(delegate: InputStream) extends ServletInputStream {
		override def read(): Int = delegate.read
		override def read(b: Array[Byte]): Int = delegate.read(b)
		override def read(b: Array[Byte], off: Int, len: Int): Int = delegate.read(b, off, len)
		override def markSupported(): Boolean = delegate.markSupported()
		override def reset(): Unit = delegate.reset()
		override def mark(readlimit: Int): Unit = delegate.mark(readlimit)
		override def close(): Unit = delegate.close()
		override def available(): Int = delegate.available()
		override def skip(n: Long): Long = delegate.skip(n)

		override def isFinished: Boolean = delegate match {
			case servletDelegate: ServletInputStream => servletDelegate.isFinished
			case _ => throw new UnsupportedOperationException
		}

		override def isReady: Boolean = delegate match {
			case servletDelegate: ServletInputStream => servletDelegate.isReady
			case _ => throw new UnsupportedOperationException
		}

		override def setReadListener(readListener: ReadListener): Unit = delegate match {
			case servletDelegate: ServletInputStream => servletDelegate.setReadListener(readListener)
			case _ => throw new UnsupportedOperationException
		}
	}
}

package uk.ac.warwick.tabula.web.filters

import java.io.ByteArrayOutputStream
import java.util.concurrent.Future

import ch.qos.logback.classic.{Level, Logger}
import com.google.common.net.MediaType
import org.junit.Ignore
import org.springframework.mock.web._
import org.springframework.util.FileCopyUtils
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.tabula.{TestBase, TestLoggerFactory}
import org.apache.http.entity.{ContentType, StringEntity}

class PostDataLoggingFilterTest extends TestBase {
	val request = new MockHttpServletRequest
	val response = new MockHttpServletResponse
	val chain = new MockFilterChain
	val filter = new PostDataLoggingFilter
	filter.multipartResolver = new StandardServletMultipartResolver

	request.setRequestURI("/url.php")

	// Capture POST_LOGGER output
	val testLogger: Logger = TestLoggerFactory.getTestLogger("POST_LOGGER")

	private def withSsoUser(user:String)(fn: =>Unit): Unit = {
		withUser(user) {
			try {
				request.setAttribute(SSOClientFilter.USER_KEY, currentUser.realUser)
				fn
			} finally {
				request.setAttribute(SSOClientFilter.USER_KEY, null)
			}
		}
	}

	@Test def noParametersAnonymous {
		assert(filter.generateLogLine(request) === "userId= multipart=false /url.php ")
	}

	@Test def noParametersLoggedIn {
		withSsoUser("ada") {
			assert(filter.generateLogLine(request) === "userId=ada multipart=false /url.php ")
		}
	}

	@Test def withParametersLoggedIn {
		request.addParameter("sql", "select SYSDATE from hedgefund where snakes='gravy'")
		request.addParameter("multiball", Array("baseball","pinball"))
		withSsoUser("beatrice") {
			assert(filter.generateLogLine(request) === "userId=beatrice multipart=false /url.php multiball=baseball&multiball=pinball&sql=select SYSDATE from hedgefund where snakes='gravy'")
		}
	}

	@Test def doFilterGet {
		filter.doFilter(request, response, chain)
		TestLoggerFactory.retrieveEvents(testLogger) should be ('empty)
	}

	@Test def doFilterPut {
		request.setMethod("PUT")
		request.addParameter("query", "acomudashun")
		filter.doFilter(request, response, chain)
		TestLoggerFactory.retrieveEvents(testLogger) should be ('empty)
	}

	@Test def doFilterPost {
		request.setMethod("POST")
		request.addParameter("query", "acomudashun")
		filter.doFilter(request, response, chain)
		val events = TestLoggerFactory.retrieveEvents(testLogger).map(e => (e.getLevel, e.getMessage))
		events should be (Seq((Level.INFO, "userId= multipart=false /url.php query=acomudashun")))
	}

	@Test(timeout = 1000)
	@Ignore("This test is broken since TAB-3840 - I think because multipart stuff happens much earlier we don't need to handle it separately any more")
	def doFilterMultipart {
		val request = new MockMultipartHttpServletRequest()
		request.setMethod("POST")

		val submissionBody = new MockMultipartFile("submission", "hello.pdf", MediaType.OCTET_STREAM.toString, Array[Byte](32,33,34,35,36,37,38))
		request.addFile(submissionBody)

		request.addParameter("confirm", "yes")

		filter.doFilter(request, response, chain)

		// Read the request completely, as the app would
		// (using the WRAPPED request passed back into the chain)
		FileCopyUtils.copyToByteArray(chain.getRequest.getInputStream)

		val future: Future[Unit] = chain.getRequest.getAttribute(filter.futureAttributeName).asInstanceOf[Future[Unit]]
		// We store the Future of the threaded task
		future.get()

		// Should only log the text fields, skip the binary parts
		// Need to use getAllLoggingEvents because it happens on another thread
		val events = TestLoggerFactory.retrieveEvents(testLogger).map(e => (e.getLevel, e.getMessage))
		events should contain ((Level.INFO, "userId= multipart=true /url.php confirm=yes"))
	}

	@Test(timeout = 1000) def doFilterJson: Unit = {
		request.setMethod("POST")

		val json = """{"academicYear": "13/14","period": "Autumn","missedPoints": {"1234567": 3}}"""

		val entity = new StringEntity(json, ContentType.APPLICATION_JSON)
		val baos = new ByteArrayOutputStream
		entity.writeTo(baos)
		request.setContentType(entity.getContentType.getValue)
		request.setContent(baos.toByteArray)

		filter.doFilter(request, response, chain)

		// Read the request completely, as the app would
		// (using the WRAPPED request passed back into the chain)
		FileCopyUtils.copyToByteArray(chain.getRequest.getInputStream)

		val future: Future[Unit] = chain.getRequest.getAttribute(filter.futureAttributeName).asInstanceOf[Future[Unit]]
		// We store the Future of the threaded task
		future.get()

		// Should only log the text fields, skip the binary parts
		// Need to use getAllLoggingEvents because it happens on another thread
		val events = TestLoggerFactory.retrieveEvents(testLogger).map(e => (e.getLevel, e.getMessage))
		events should contain ((Level.INFO, s"userId= multipart=false /url.php requestBody=$json"))
	}
}

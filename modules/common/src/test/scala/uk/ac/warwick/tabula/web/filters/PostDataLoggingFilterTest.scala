package uk.ac.warwick.tabula.web.filters

import java.io.{ByteArrayOutputStream, StringWriter}
import java.util.concurrent.Future

import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.ByteArrayBody
import org.apache.log4j.{PatternLayout, Level, WriterAppender}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletResponse, MockHttpServletRequest}
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.TestBase
import org.apache.http.entity.ContentType

class PostDataLoggingFilterTest extends TestBase {
	val request = new MockHttpServletRequest
	val response = new MockHttpServletResponse
	val chain = new MockFilterChain
	val filter = new PostDataLoggingFilter

	request.setRequestURI("/url.php")

	// Capture POST_LOGGER output into a StringWriter.
	val writer = new StringWriter()
	val appender = new WriterAppender(new PatternLayout("%c - %m%n"), writer)
	filter.postLogger.setLevel(Level.INFO)
	filter.postLogger.addAppender(appender)

	@Test def noParametersAnonymous {
		assert(filter.generateLogLine(request) === "userId= multipart=false /url.php ")
	}

	@Test def noParametersLoggedIn {
		withUser("ada") {
			assert(filter.generateLogLine(request) === "userId=ada multipart=false /url.php ")
		}
	}

	@Test def withParametersLoggedIn {
		request.addParameter("sql", "select SYSDATE from hedgefund where snakes='gravy'")
		request.addParameter("multiball", Array("baseball","pinball"))
		withUser("beatrice") {
			assert(filter.generateLogLine(request) === "userId=beatrice multipart=false /url.php multiball=baseball&multiball=pinball&sql=select SYSDATE from hedgefund where snakes='gravy'")
		}
	}

	@Test def doFilterGet {
		filter.doFilter(request, response, chain)
		assert(writer.toString === "")
	}

	@Test def doFilterPut {
		request.setMethod("PUT")
		request.addParameter("query", "acomudashun")
		filter.doFilter(request, response, chain)
		assert(writer.toString === "")
	}

	@Test def doFilterPost {
		request.setMethod("POST")
		request.addParameter("query", "acomudashun")
		filter.doFilter(request, response, chain)
		assert(writer.toString === "POST_LOGGER - userId= multipart=false /url.php query=acomudashun\n")
	}

	@Test(timeout = 1000) def doFilterMultipart {
		request.setMethod("POST")

		val submissionBody = new ByteArrayBody(Array[Byte](32,33,34,35,36,37,38), ContentType.APPLICATION_OCTET_STREAM, "hello.pdf")

		val entity = MultipartEntityBuilder.create
			.setBoundary("-----woooop-----")
			.addTextBody("confirm","yes")
			.addPart("submission", submissionBody)
			.build()

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
		assert(writer.toString === "POST_LOGGER - userId= multipart=true /url.php confirm=yes\n")
	}
}

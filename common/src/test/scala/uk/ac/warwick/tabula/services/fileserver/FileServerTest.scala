package uk.ac.warwick.tabula.services.fileserver

import com.google.common.net.MediaType
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.{FeaturesImpl, TestBase, Mockito}
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.{FileInputStream, InputStream, File}
import org.springframework.util.FileCopyUtils
import org.joda.time.{Hours, DateTime}
import org.junit.Before

class FileServerTest extends TestBase with Mockito {

	val server = new FileServer
	server.features = new FeaturesImpl

	val content = "file content"

	val tmpFile: File = File.createTempFile("fileservertest", ".txt")
	FileCopyUtils.copy(content.getBytes("UTF-8"), tmpFile)

	@Test def streamEmptyAttachment {
		implicit val req = new MockHttpServletRequest
		implicit val res = new MockHttpServletResponse

		val a = new FileAttachment
		a.id = "123"
		a.objectStorageService = smartMock[ObjectStorageService]

		a.objectStorageService.fetch("123") returns None
		a.objectStorageService.metadata("123") returns None

		val file = new RenderableAttachment(a)

		server.stream(file)

		res.getContentLength() should be (0)
		res.getContentType() should be (MediaType.OCTET_STREAM.toString)
		res.getContentAsByteArray().length should be (0)
	}

	@Test def streamAttachment {
		implicit val req = new MockHttpServletRequest
		implicit val res = new MockHttpServletResponse

		val a = new FileAttachment
		a.id = "123"
		a.objectStorageService = smartMock[ObjectStorageService]

		a.objectStorageService.fetch("123") returns Some(new FileInputStream(tmpFile))
		a.objectStorageService.metadata("123") returns Some(ObjectStorageService.Metadata(contentLength = content.length, contentType = MediaType.OCTET_STREAM.toString, fileHash = None))

		val file = new RenderableAttachment(a)

		server.stream(file)

		res.getContentLength() should be (content.length)
		res.getHeader("Content-Length") should be (content.length.toString)
		res.getContentType() should be (MediaType.OCTET_STREAM.toString)
		res.getHeader("Content-Disposition") should be (null)
		res.getContentAsString() should be (content)
	}

	@Test def serveAttachment {
		implicit val req = new MockHttpServletRequest
		implicit val res = new MockHttpServletResponse

		val a = new FileAttachment
		a.id = "123"
		a.objectStorageService = smartMock[ObjectStorageService]

		a.objectStorageService.fetch("123") returns Some(new FileInputStream(tmpFile))
		a.objectStorageService.metadata("123") returns Some(ObjectStorageService.Metadata(contentLength = content.length, contentType = MediaType.OCTET_STREAM.toString, fileHash = None))

		val file = new RenderableAttachment(a)

		server.serve(file)

		res.getContentLength() should be (content.length)
		res.getHeader("Content-Length") should be (content.length.toString)
		res.getContentType() should be (MediaType.OCTET_STREAM.toString)
		res.getHeader("Content-Disposition") should be ("attachment")
		res.getContentAsString() should be (content)
	}

	@Test def streamHead {
		implicit val req = new MockHttpServletRequest
		req.setMethod("HEAD")

		implicit val res = new MockHttpServletResponse

		val a = new FileAttachment
		a.id = "123"
		a.objectStorageService = smartMock[ObjectStorageService]

		a.objectStorageService.fetch("123") returns Some(new FileInputStream(tmpFile))
		a.objectStorageService.metadata("123") returns Some(ObjectStorageService.Metadata(contentLength = content.length, contentType = "application/zip", fileHash = None))

		val file = new RenderableAttachment(a)

		server.stream(file)

		res.getContentLength() should be (content.length)
		res.getHeader("Content-Length") should be (content.length.toString)
		res.getContentType() should be (MediaType.OCTET_STREAM.toString)
		res.getHeader("Content-Disposition") should be (null)
		res.getContentAsByteArray().length should be (0)
	}

	@Test def serveHead {
		implicit val req = new MockHttpServletRequest
		req.setMethod("HEAD")

		implicit val res = new MockHttpServletResponse

		val a = new FileAttachment
		a.id = "123"
		a.objectStorageService = smartMock[ObjectStorageService]

		a.objectStorageService.fetch("123") returns Some(new FileInputStream(tmpFile))
		a.objectStorageService.metadata("123") returns Some(ObjectStorageService.Metadata(contentLength = content.length, contentType = "application/zip", fileHash = None))

		val file = new RenderableAttachment(a)

		server.serve(file)

		res.getContentLength() should be (content.length)
		res.getHeader("Content-Length") should be (content.length.toString)
		res.getContentType() should be (MediaType.OCTET_STREAM.toString)
		res.getHeader("Content-Disposition") should be ("attachment")
		res.getContentAsByteArray().length should be (0)
	}

	@Test def expiresHeader {
		implicit val req = new MockHttpServletRequest
		implicit val res = mock[MockHttpServletResponse]

		val time = new DateTime(2012, 6, 7, 8, 9, 10, 0)
		val period = Hours.THREE

		val file = mock[RenderableFile]
		file.cachePolicy returns (CachePolicy(expires = Some(period)))
		file.contentLength returns None

		withFakeTime(time) {
			server.serve(file)(req, res)
		}

		verify(res, times(1)).setDateHeader("Expires", time.plus(period).getMillis)
	}

}
package uk.ac.warwick.tabula.services.fileserver
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletResponse
import org.springframework.util.FileCopyUtils
import javax.servlet.http.HttpServletRequest
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

@Service
class FileServer extends StreamsFiles with AutowiringFeaturesComponent {

	/**
	 * Serves a RenderableFile out to an HTTP response.
	 */
	def serve(file: RenderableFile, fileName: Option[String] = None)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		/*
		 * There's no consistent standard for encoding in the optional
		 * "filename" attribute of Content-Disposition, so you should stick
		 * to the only reliable method of specifying the filename which
		 * is to put it as the last part of the URL path.
		 */
		val dispositionHeader = fileName match {
			case Some(name) => "attachment;filename=\"" + name + "\""
			case _ => "attachment"
		}

		out.addHeader("Content-Disposition", dispositionHeader)

		stream(file)
	}
}

/**
 * Sets up the appropriate response headers and streams a renderable file
 *
 * We don't support using the content disposition header here as it's an unreliable way of setting the filename
 */
trait StreamsFiles {

	this: FeaturesComponent =>

	def stream(file: RenderableFile)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		val inStream = file.inputStream

		out.addHeader("Content-Type", file.contentType)

		handleCaching(file, request, out)

		if (request.getMethod.toUpperCase != "HEAD") {
			file.contentLength.foreach { length => out.addHeader("Content-Length", length.toString) }
			Option(inStream).foreach { FileCopyUtils.copy(_, out.getOutputStream) }
		} else {
			file.contentLength.foreach { length => out.addHeader("Content-Length", length.toString) }
		}
	}

	// Very simplistic cache headers - needs turbocharging with TAB-929
	private def handleCaching(file: RenderableFile, request: HttpServletRequest, out: HttpServletResponse) {
		for (expires <- file.cachePolicy.expires) {
			out.setDateHeader("Expires", (DateTime.now plus expires).getMillis)
		}
	}
}
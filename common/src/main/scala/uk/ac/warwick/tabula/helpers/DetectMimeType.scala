package uk.ac.warwick.tabula.helpers

import java.io.InputStream

import org.apache.tika.detect.DefaultDetector
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MimeTypes
import uk.ac.warwick.tabula.helpers.Closeables._

object DetectMimeType {
	private val detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes)

	def detectMimeType(in: InputStream): String = {
		closeThis(TikaInputStream.get(in)) { tikaIS =>
			detector.detect(tikaIS, new Metadata).toString
		}
	}
}
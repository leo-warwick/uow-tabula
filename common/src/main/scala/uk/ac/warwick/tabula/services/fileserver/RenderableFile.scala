package uk.ac.warwick.tabula.services.fileserver
import java.io.InputStream
import java.io.File

trait RenderableFile {
	def inputStream: InputStream
	def filename: String
	def contentType: String
	def contentLength: Option[Long]

	def cachePolicy = CachePolicy()
}
package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream

import com.google.common.io.ByteSource
import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.jclouds.blobstore.domain.{Blob, StorageMetadata}
import org.jclouds.blobstore.options.{ListContainerOptions, PutOptions}
import org.jclouds.blobstore.strategy.internal.MultipartUploadSlicingAlgorithm
import org.jclouds.io.internal.BasePayloadSlicer
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

/**
	* Implementation uses the Apache jclouds library to push and pull data to the cloud store
	*/
class BlobStoreObjectStorageService(blobStoreContext: BlobStoreContext, objectContainerName: String)
	extends ObjectStorageService with Logging with InitializingBean with TaskBenchmarking {

	protected lazy val blobStore: BlobStore = blobStoreContext.getBlobStore

	private final val slicer = new BasePayloadSlicer

	override def afterPropertiesSet(): Unit = {
		// Create the container if it doesn't exist
		if (!blobStore.containerExists(objectContainerName)) blobStore.createContainerInLocation(null, objectContainerName)
	}

	override def keyExists(key: String): Boolean = blobStore.blobExists(objectContainerName, key)

	private def blob(key: String) = key.maybeText.flatMap { k => Option(blobStore.getBlob(objectContainerName, k)) }

	override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit = benchmark(s"Push file for key $key", level = Logging.Level.Debug) {
		Assert.notNull(key, "Key must be defined")

		val blob = blobStore.blobBuilder(key)
			.payload(in)
			.contentDisposition(key)
			.contentType(metadata.contentType)
			.contentLength(metadata.contentLength)
			.userMetadata(metadata.fileHash.map { h => "shahex" -> h }.toMap.asJava)
			.build()

		// TAB-4144 Use large object support for anything over 50mb
		// TAB-4235 If you want this done in parallel, you have to do it yourself
		if (metadata.contentLength > 50 * 1024 * 1024) {
			val partSize =
				new MultipartUploadSlicingAlgorithm(blobStore.getMinimumMultipartPartSize, blobStore.getMaximumMultipartPartSize, blobStore.getMaximumNumberOfParts)
					.calculateChunkSize(metadata.contentLength)

			val multipartUpload = blobStore.initiateMultipartUpload(objectContainerName, blob.getMetadata, PutOptions.NONE)

			val parts = slicer.slice(blob.getPayload, partSize).asScala.toList.zipWithIndex.par.map { case (payload, index) =>
				blobStore.uploadMultipartPart(multipartUpload, index + 1, payload)
			}.seq

			benchmarkTask(s"blobstore upload multipart $key size=${metadata.contentLength}") {
				blobStore.completeMultipartUpload(multipartUpload, parts.asJava)
			}
		} else {
			benchmarkTask(s"blobstore upload single $key size=${metadata.contentLength}") {
				blobStore.putBlob(objectContainerName, blob, PutOptions.NONE)
			}
		}
	}

	/**
		* Overridden in SwiftObjectStorageService to support multipart
		*/
	protected def putBlob(blob: Blob, metadata: ObjectStorageService.Metadata): String = blobStore.putBlob(objectContainerName, blob)

	override def fetch(key: String): Option[InputStream] = benchmark(s"Fetch key $key", level = Logging.Level.Debug) {
		blob(key).map { _.getPayload.openStream() }
	}

	override def metadata(key: String): Option[ObjectStorageService.Metadata] = benchmark(s"Metadata key $key", level = Logging.Level.Debug) {
		blob(key).map { blob =>
			val contentMetadata = blob.getPayload.getContentMetadata
			val metadata = blob.getMetadata

			ObjectStorageService.Metadata(
				contentLength = contentMetadata.getContentLength,
				contentType = contentMetadata.getContentType,
				fileHash = metadata.getUserMetadata.get("shahex").maybeText
			)
		}
	}

	def delete(key: String): Unit = benchmark(s"Deleting key $key", level = Logging.Level.Debug) {
		blobStore.removeBlob(objectContainerName, key)
	}

	override def listKeys(): Stream[String] = benchmark("List keys", level = Logging.Level.Debug) {
		def list(nextMarker: Option[String], accumulator: Stream[_ <: StorageMetadata]): Stream[_ <: StorageMetadata] = nextMarker match {
			case None => // No more results
				accumulator

			case Some(marker) =>
				accumulator.append({
					val nextResults = blobStore.list(objectContainerName, ListContainerOptions.Builder.afterMarker(marker))
					list(Option(nextResults.getNextMarker), nextResults.asScala.toStream)
				})
		}

		val firstResults = blobStore.list(objectContainerName)
		list(Option(firstResults.getNextMarker), firstResults.asScala.toStream).map {
			_.getName
		}
	}
}

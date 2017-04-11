package uk.ac.warwick.tabula.services.objectstore

import java.io.{File, InputStream}
import java.util.Properties

import com.google.common.io.ByteSource
import com.google.common.net.MediaType
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.{BlobStoreContext, TransientApiMetadata}
import org.jclouds.filesystem.FilesystemApiMetadata
import org.jclouds.filesystem.reference.FilesystemConstants
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.openstack.swift.v1.SwiftApiMetadata
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

import scala.collection.JavaConverters._

object ObjectStorageService {
	case class Metadata(
		contentLength: Long,
		contentType: String,
		fileHash: Option[String]
	)
}

trait ObjectStorageService extends InitializingBean {
	def keyExists(key: String): Boolean

	def fetch(key: String): Option[InputStream]

	def metadata(key: String): Option[ObjectStorageService.Metadata]

	/**
		* Combines calls to fetch() and metadata()
		*/
	def renderable(key: String, fileName: Option[String]): Option[RenderableFile] =
		if (keyExists(key)) {
			Some(new RenderableFile {
				override val filename: String = fileName.getOrElse(key)

				private lazy val fileMetadata = metadata(key)

				override lazy val contentLength: Option[Long] = fileMetadata.map(_.contentLength)

				override lazy val contentType: String = fileMetadata.map(_.contentType).getOrElse(MediaType.OCTET_STREAM.toString)

				override def inputStream: InputStream = fetch(key).orNull
			})
		} else None

	def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit

	def delete(key: String): Unit

	def listKeys(): Stream[String]
}

@Service
class ObjectStorageServiceFactoryBean extends ScalaFactoryBean[ObjectStorageService] {

	// As this a singleton, this reference will be retained by the bean factory,
	// so don't need to worry about it going away
	@transient private var blobStoreContext: BlobStoreContext = _

	@Value("${objectstore.container}") var containerName: String = _
	@Value("${objectstore.provider}") var providerType: String = _

	// This defaults to an empty string
	@Value("${filesystem.attachment.dir:}") var legacyAttachmentsDirectory: String = _

	private def createFilesystemBlobContext(): ContextBuilder = {
		val properties = {
			val p = new Properties
			p.setProperty(FilesystemConstants.PROPERTY_BASEDIR, Wire.property("${objectstore.filesystem.baseDir}"))
			p
		}

		ContextBuilder.newBuilder(new FilesystemApiMetadata)
			.overrides(properties)
	}

	private def createSwiftBlobContext(): ContextBuilder = {
		val endpoint = Wire.property("${objectstore.swift.endpoint}")
		val username = Wire.property("${objectstore.swift.username}")
		val password = Wire.property("${objectstore.swift.password}")

		ContextBuilder.newBuilder(new SwiftApiMetadata)
			.endpoint(endpoint)
			.credentials(s"LDAP_$username:$username", password)
	}

	override def createInstance(): ObjectStorageService = {
		val contextBuilder = (providerType match {
			case "filesystem" => createFilesystemBlobContext()
			case "swift" => createSwiftBlobContext()
			case "transient" => ContextBuilder.newBuilder(new TransientApiMetadata)
			case _ => throw new IllegalArgumentException(s"Invalid provider type $providerType")
		}).modules(Seq(new SLF4JLoggingModule).asJava)

		blobStoreContext = contextBuilder.buildView(classOf[BlobStoreContext])

		val blobStoreService = new BlobStoreObjectStorageService(blobStoreContext, containerName)

		legacyAttachmentsDirectory.maybeText.map(new File(_)) match {
			case Some(dir) => new LegacyAwareObjectStorageService(
				defaultService = blobStoreService,
				legacyService = new LegacyFilesystemObjectStorageService(dir)
			)

			case _ => blobStoreService
		}
	}

	override def destroy(): Unit = blobStoreContext.close()

}

trait ObjectStorageServiceComponent {
	def objectStorageService: ObjectStorageService
}

trait AutowiringObjectStorageServiceComponent extends ObjectStorageServiceComponent {
	var objectStorageService: ObjectStorageService = Wire[ObjectStorageService]
}
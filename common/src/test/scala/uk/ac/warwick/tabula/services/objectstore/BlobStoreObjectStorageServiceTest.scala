package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream
import java.security.{DigestInputStream, MessageDigest}

import com.google.common.io.ByteSource
import org.apache.commons.io.IOUtils
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.domain.internal.{PageSetImpl, StorageMetadataImpl}
import org.jclouds.blobstore.domain.{PageSet, StorageMetadata, StorageType}
import org.jclouds.blobstore.options.ListContainerOptions
import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.util.files.hash.impl.SHAFileHasher

import scala.collection.JavaConverters._

class BlobStoreObjectStorageServiceTest extends TestBase with Mockito {

	private trait ListKeysFixture {
    val containerName = "tabula"

		val blobStoreContext: BlobStoreContext = mock[BlobStoreContext]
		val blobStore: BlobStore = mock[BlobStore]

		when(blobStoreContext.getBlobStore) thenReturn blobStore

    val service = new BlobStoreObjectStorageService(blobStoreContext, containerName)
		service.afterPropertiesSet()

    val metadata1 = new StorageMetadataImpl(StorageType.BLOB, "id1", "1", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata2 = new StorageMetadataImpl(StorageType.BLOB, "id2", "2", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata3 = new StorageMetadataImpl(StorageType.BLOB, "id3", "3", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata4 = new StorageMetadataImpl(StorageType.BLOB, "id4", "4", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata5 = new StorageMetadataImpl(StorageType.BLOB, "id5", "5", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata6 = new StorageMetadataImpl(StorageType.BLOB, "id6", "6", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata7 = new StorageMetadataImpl(StorageType.BLOB, "id7", "7", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata8 = new StorageMetadataImpl(StorageType.BLOB, "id8", "8", null, null, null, null, null, Map[String, String]().asJava, null)
    val metadata9 = new StorageMetadataImpl(StorageType.BLOB, "id9", "9", null, null, null, null, null, Map[String, String]().asJava, null)

    val results1 = new PageSetImpl(Seq(metadata1, metadata2, metadata3).asJava, "after1")
    val results2 = new PageSetImpl(Seq(metadata4, metadata5, metadata6).asJava, "after2")
    val results3 = new PageSetImpl(Seq(metadata7, metadata8, metadata9).asJava, null)

		when[PageSet[_ <: StorageMetadata]](blobStore.list(containerName)) thenReturn results1
		when[PageSet[_ <: StorageMetadata]](blobStore.list(containerName, ListContainerOptions.Builder.afterMarker("after1"))) thenReturn results2
		when[PageSet[_ <: StorageMetadata]](blobStore.list(containerName, ListContainerOptions.Builder.afterMarker("after2"))) thenReturn results3
  }

	@Test def listKeys(): Unit = new ListKeysFixture {
		service.listKeys().force.toList should be (List("1", "2", "3", "4", "5", "6", "7", "8", "9"))

		verify(blobStore, times(1)).list(containerName)
		verify(blobStore, times(1)).list(containerName, ListContainerOptions.Builder.afterMarker("after1"))
		verify(blobStore, times(1)).list(containerName, ListContainerOptions.Builder.afterMarker("after2"))
	}

	@Test def listKeysLazy(): Unit = new ListKeysFixture {
		service.listKeys().take(5).force.toList should be (List("1", "2", "3", "4", "5"))

		verify(blobStore, times(1)).list(containerName)
		verify(blobStore, times(1)).list(containerName, ListContainerOptions.Builder.afterMarker("after1"))
		verify(blobStore, never()).list(containerName, ListContainerOptions.Builder.afterMarker("after2"))
	}

	private trait TransientBlobStoreFixture {
		val containerName = "tabula"
		val blobStoreContext: BlobStoreContext = ContextBuilder.newBuilder("transient").buildView(classOf[BlobStoreContext])

		val service = new BlobStoreObjectStorageService(blobStoreContext, "tabula")
		service.afterPropertiesSet()

		val byteSource = new ByteSource {
			override def openStream(): InputStream = getClass.getResourceAsStream("/attachment1.docx")
		}
	}

	@Test def pushAndFetch(): Unit = new TransientBlobStoreFixture {
		val key = "my-lovely-file"

		val md: MessageDigest = MessageDigest.getInstance("MD5")
		val dis = new DigestInputStream(byteSource.openStream(), md)
		IOUtils.toByteArray(dis) // pass over the bytes for the DigestInputStream
		val originalMd5: Array[Byte] = md.digest()

		service.push(key, byteSource, ObjectStorageService.Metadata(
			contentLength = 14949,
			contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			fileHash = None
		))

		val fetchedFile: Option[InputStream] = service.fetch(key)
		fetchedFile should be ('defined)

		val fetchedMd5: Array[Byte] = MessageDigest.getInstance("MD5").digest(IOUtils.toByteArray(fetchedFile.get))
		originalMd5 should be (fetchedMd5)
	}

	@Test def metadata(): Unit = new TransientBlobStoreFixture {
		val key = "my-lovely-file"

		service.metadata(key) should be (None)
		service.push(key, byteSource, ObjectStorageService.Metadata(
			contentLength = 14949,
			contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			fileHash = Some(new SHAFileHasher().hash(byteSource.openStream()))
		))

		service.metadata(key) should be (Some(ObjectStorageService.Metadata(
			contentLength = 14949,
			contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			fileHash = Some("f992551ba3325d20a529f0821375ca0b544a4598")
		)))
	}

	@Test def exists(): Unit = new TransientBlobStoreFixture {
		val key = "my-lovely-file"

		service.keyExists(key) should be (false)
		service.push(key, byteSource, ObjectStorageService.Metadata(
			contentLength = 14949,
			contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			fileHash = None
		))
		service.keyExists(key) should be (true)
	}

	@Test def listKeysTransient(): Unit = new TransientBlobStoreFixture {
		val key = "my-lovely-file"

		service.listKeys() should be ('empty)
		service.push(key, byteSource, ObjectStorageService.Metadata(
			contentLength = 14949,
			contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			fileHash = None
		))
		service.listKeys().toList should be (List(key))
	}

}

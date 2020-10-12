package uk.ac.warwick.tabula.data

import java.io.InputStream
import java.nio.charset.StandardCharsets

import com.google.common.io.ByteSource
import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.services.objectstore.{ObjectStorageService, RichByteSource}
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase}

import scala.concurrent.Future

// scalastyle:off magic.number
@Transactional
class FileDaoTest extends PersistenceTestBase with Mockito {

  val dao = new FileDao
  val objectStorageService: ObjectStorageService = smartMock[ObjectStorageService]

  @Before def setup(): Unit = {
    dao.objectStorageService = objectStorageService
    objectStorageService.keyExists(any[String]) returns Future.successful(false)
    objectStorageService.push(any[String], any[ByteSource], any[ObjectStorageService.Metadata]) returns Future.successful(())
    dao.sessionFactory = sessionFactory
  }

  @Test def deletingTemporaryFiles(): Unit = withFakeTime(new DateTime(2012, DateTimeConstants.JANUARY, 15, 1, 0, 0, 0)) {
    transactional { transactionStatus =>
      for (i <- 0 to 50) {
        val attachment = new FileAttachment
        attachment.dateUploaded = new DateTime().plusHours(1).minusDays(i)
        attachment.uploadedData = ByteSource.wrap("This is the best file ever".getBytes)
        attachment.fileDao = dao
        attachment.objectStorageService = objectStorageService
        dao.saveTemporary(attachment)
      }
    }
    transactional { _ =>
      dao.deleteOldTemporaryFiles() should be(36) // 50 files, 14 days of leeway
    }
  }

  @After def bangtidy(): Unit = transactional { tx =>
    session.createQuery("delete from FileAttachment").executeUpdate()
  }

  @Test def crud(): Unit = transactional { tx =>
    val attachments = for (i <- 1 to 10) yield {
      val attachment = new FileAttachment
      attachment.dateUploaded = new DateTime(2013, DateTimeConstants.FEBRUARY, i, 1, 0, 0, 0)
      attachment.uploadedData = ByteSource.wrap("This is the best file ever".getBytes)
      attachment.fileDao = dao
      attachment.objectStorageService = objectStorageService
      dao.savePermanent(attachment)

      attachment.hash should be("f95a27f06df98ba26182c22e277af960c0be9be6")

      attachment
    }

    for (attachment <- attachments) {
      dao.getFileById(attachment.id) should be(Some(attachment))
      dao.getFileByStrippedId(attachment.id.replaceAll("\\-", "")) should be(Some(attachment))
      dao.getFilesCreatedOn(attachment.dateUploaded, 10, "") should be(Seq(attachment))
      dao.getFilesCreatedOn(attachment.dateUploaded, 10, attachment.id) should be(Seq())
    }

    dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be(Seq(attachments.head))
    dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 1) should be(Seq(attachments(4)))
    dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 10) should be(attachments.slice(4, 10))

    dao.getAllFileIds() should be((attachments.map(_.id)).toSet)
    dao.getAllFileIds(Some(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0))) should be((attachments.slice(0, 4).map(_.id)).toSet)
  }

  @Test
  def save(): Unit = {
    transactional { tx =>
      val attachment = new FileAttachment("file.txt")
      val string = "Doe, a deer, a female deer"
      val bytes = string.getBytes("UTF-8")
      attachment.fileDao = dao
      attachment.objectStorageService = objectStorageService
      attachment.uploadedData = ByteSource.wrap(bytes)

      dao.saveTemporary(attachment)
      verify(objectStorageService, times(1)).push(attachment.id, attachment.uploadedData, ObjectStorageService.Metadata(26, "text/plain", Some("4931c07015e50baf297aae2ce8571da15c0f2380")))

      attachment.id should not be (null)

      session.flush()
      session.clear()

      dao.getFileById(attachment.id) match {
        case Some(loadedAttachment: FileAttachment) => {
          //val blob = loadedAttachment.data
          loadedAttachment.fileDao = dao
          loadedAttachment.objectStorageService = objectStorageService
          loadedAttachment.objectStorageService.fetch(attachment.id) returns Future.successful(RichByteSource.wrap(ByteSource.wrap(bytes), None))

          val data = loadedAttachment.asByteSource.asCharSource(StandardCharsets.UTF_8).read()
          data should be(string)
        }
        case None => fail("nope")
      }
    }
  }

  private def readStream(is: InputStream, encoding: String) = new String(FileCopyUtils.copyToByteArray(is), encoding)

}

package uk.ac.warwick.tabula.data

import java.io.ByteArrayInputStream

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase}

// scalastyle:off magic.number
@Transactional
class FileDaoTest extends PersistenceTestBase with Mockito {

	val dao = new FileDao

	@Before def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def deletingTemporaryFiles() = withFakeTime(new DateTime(2012, DateTimeConstants.JANUARY, 15, 1, 0, 0, 0)) {
		transactional { transactionStatus =>
			for (i <- 0 to 50) {
				val attachment = new FileAttachment
				attachment.dateUploaded = new DateTime().plusHours(1).minusDays(i)
				attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
				dao.saveOrUpdate(attachment)
			}
		}
		transactional { transactionStatus =>
			dao.findOldTemporaryFiles(15, 50) should be (36) // 50 files, 14 days of leeway
		}
	}

	@After def bangtidy() { transactional { tx =>
		session.newQuery("delete from FileAttachment").executeUpdate()
	}}

	@Test def crud(): Unit = transactional { tx =>
		val attachments = for (i <- 1 to 10) yield {
			val attachment = new FileAttachment
			attachment.dateUploaded = new DateTime(2013, DateTimeConstants.FEBRUARY, i, 1, 0, 0, 0)
			attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
			dao.saveOrUpdate(attachment)

			attachment.hash should be ("f95a27f06df98ba26182c22e277af960c0be9be6")

			attachment
		}

		for (attachment <- attachments) {
			dao.getFileById(attachment.id) should be (Some(attachment))
			dao.getFileByStrippedId(attachment.id.replaceAll("\\-", "")) should be (Some(attachment))
			dao.getFilesCreatedOn(attachment.dateUploaded, 10, "") should be (Seq(attachment))
			dao.getFilesCreatedOn(attachment.dateUploaded, 10, attachment.id) should be (Seq())
		}

		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(attachments.head))
		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 1) should be (Seq(attachments(4)))
		dao.getFilesCreatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 10) should be (attachments.slice(4, 10))

		dao.getAllFileIds() should be ((attachments map { _.id }).toSet)
		dao.getAllFileIds(Some(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0))) should be ((attachments.slice(0, 4) map { _.id }).toSet)
	}

}
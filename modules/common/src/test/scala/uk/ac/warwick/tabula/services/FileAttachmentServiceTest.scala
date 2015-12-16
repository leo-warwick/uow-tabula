package uk.ac.warwick.tabula.services

import java.io.{ByteArrayInputStream, File}

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.{FileDao, FileDaoComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}

class FileAttachmentServiceTest extends TestBase with Mockito {

	trait Fixture {
		val service = new AbstractFileAttachmentService with FileDaoComponent with SHAFileHasherComponent {
			val fileDao = smartMock[FileDao]
		}

		service.attachmentDir = createTemporaryDirectory()
	}

	@Test def deletingTemporaryFiles(): Unit = withFakeTime(new DateTime(2012, DateTimeConstants.JANUARY, 15, 1, 0, 0, 0)) { new Fixture {
		service.attachmentDir.list.size should be (0)

		val attachments = for (i <- 0 to 50) yield {
			val attachment = new FileAttachment
			attachment.dateUploaded = new DateTime().plusHours(1).minusDays(i)
			attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
			attachment.fileAttachmentService = service
			service.saveTemporary(attachment)
		}

		service.deleteOldTemporaryFiles should be (36) // 50 files, 14 days of leeway
	}}

	@Test def savePermanent(): Unit = new Fixture {
		service.attachmentDir.list.size should be (0)

		val attachments = for (i <- 1 to 10) yield {
			val attachment = new FileAttachment
			attachment.dateUploaded = new DateTime(2013, DateTimeConstants.FEBRUARY, i, 1, 0, 0, 0)
			attachment.uploadedData = new ByteArrayInputStream("This is the best file ever".getBytes)
			attachment.fileAttachmentService = service
			service.savePermanent(attachment)

			attachment.hash should be ("f95a27f06df98ba26182c22e277af960c0be9be6")

			attachment
		}

		service.attachmentDir.list.size should be (10)
	}

	/*
	 * TAB-202 changes the storage to split the path every 2 characters
	 * instead of every 4. This checks that we work with 2 characters for new
	 * data but can still find existing data stored under the old location.
	 */
	@Test
	def compatDirectorySplit(): Unit = new Fixture {
		// Create some fake files, of new and old format
		val paths = Seq(
			"aaaa/bbbb/dddd/eeee",
			"aaaa/bbbb/cccc/dddd",
			"aa/aa/bb/bb/cc/cc/ef/ef")
		for (path <- paths) {
			val file = new File(service.attachmentDir, path)
			assert( file.getParentFile.exists || file.getParentFile.mkdirs() )
			assert( file.createNewFile() )
		}

		def getRelativePath(file: File) = {
			val prefix = service.attachmentDir.getAbsolutePath
			file.getAbsolutePath.replace(prefix, "")
		}

		getRelativePath( service.getData("aaaabbbbccccdddd").orNull ) should be (File.separator + "aaaa" + File.separator + "bbbb" + File.separator  + "cccc" + File.separator + "dddd")
		getRelativePath( service.getData("aaaabbbbddddeeee").orNull ) should be (File.separator + "aaaa" + File.separator + "bbbb" + File.separator + "dddd" + File.separator + "eeee")
		getRelativePath( service.getData("aaaabbbbccccefef").orNull ) should be (File.separator + "aa" + File.separator + "aa" + File.separator + "bb" + File.separator + "bb" + File.separator + "cc" + File.separator + "cc" + File.separator + "ef" + File.separator + "ef")
	}

}

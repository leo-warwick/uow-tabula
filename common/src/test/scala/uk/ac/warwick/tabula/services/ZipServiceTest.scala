package uk.ac.warwick.tabula.services

import java.io.FileInputStream

import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService

import collection.JavaConverters._
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import java.util.zip.ZipInputStream
import org.springframework.core.io.ClassPathResource
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import uk.ac.warwick.userlookup.User
import org.junit.Before
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

class ZipServiceTest extends TestBase with Mockito {

	var userDatabase: Seq[User] = Seq(
		("0000000","aaslat","aaaaa"),
		("0000001","baslat","aaaab"),
		("0000002","caslat","aaaac"),
		("0000003","daslat","aaaad"),
		("0000004","easlat","aaaae"),
		("0000005","faslat","aaaaf"),
		("0000006","gaslat","aaaag"),
		("0000007","haslat","aaaah")
	) map { case(warwickId,userId,code) =>
		val user = new User(code)
		user.setWarwickId(warwickId)
		user.setUserId(userId)
		user.setFullName("Roger " + code.head.toUpper + code.tail)
		user
	}


	var userLookup: UserLookupService = _

	@Before def before() {
		userLookup = mock[UserLookupService]

		userLookup.getUserByUserId(any[String]) answers { id =>
			userDatabase find {_.getUserId == id} getOrElse new AnonymousUser()
		}
		userLookup.getUserByWarwickUniId(any[String]) answers { id =>
			userDatabase find {_.getWarwickId == id} getOrElse new AnonymousUser()
		}
	}


	@Test def generateSubmissionDownload() {
		val service = new ZipService
		service.objectStorageService = createTransientObjectStore()
		service.features = emptyFeatures
		service.userLookup = userLookup

		val module = new Module(code="ph105", adminDepartment=new Department)

		val assignment = new Assignment
		val submission = new Submission

		submission._universityId = "0000007"
		submission.usercode = "haslat"
		submission.assignment = assignment

		val attachment = new FileAttachment
		attachment.name = "garble.doc"

		val backingFile = createTemporaryFile()
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(backingFile.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(backingFile)) }

		submission.values = Set(SavedFormValue.withAttachments(submission, "files", Set(attachment))).asJava
		assignment.module = module
		val items = service.getSubmissionZipItems(submission)
		items.size should be (1)
		items.head.name should be ("ph105 - 0000007 - garble.doc")
	}

	@Test def generateSubmissionDownloadFullNamePrefix() {
		val service = new ZipService
		service.objectStorageService = createTransientObjectStore()
		service.features = emptyFeatures
		service.userLookup = userLookup

		val department = new Department
		department.showStudentName = true

		val module = new Module(code="ph105", adminDepartment=department)

		val assignment = new Assignment
		assignment.module = module

		val submission = new Submission
		submission._universityId = "0000007"
		submission.usercode = "haslat"
		submission.assignment = assignment

		val attachment = new FileAttachment
		attachment.name = "garble.doc"

		val backingFile = createTemporaryFile()
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(backingFile.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(backingFile)) }

		submission.values = Set(SavedFormValue.withAttachments(submission, "files", Set(attachment))).asJava
		assignment.module = module
		val items = service.getSubmissionZipItems(submission)
		items.size should be (1)
		items.head.name should be ("ph105 - Roger Aaaah - 0000007 - garble.doc")
	}



	@Test def generateSubmissionDownloadUserLookupFail() {
		val service = new ZipService
		service.objectStorageService = createTransientObjectStore()
		service.features = emptyFeatures
		service.userLookup = userLookup

		val department = new Department
		department.showStudentName = true

		val module = new Module(code="ph105", adminDepartment=department)

		val assignment = new Assignment
		assignment.module = module

		val submission = new Submission
		submission._universityId = "0000007"
		submission.usercode = ""
		submission.assignment = assignment

		val attachment = new FileAttachment
		attachment.name = "garble.doc"

		val backingFile = createTemporaryFile()
		attachment.objectStorageService = smartMock[ObjectStorageService]
		attachment.objectStorageService.keyExists(attachment.id) returns true
		attachment.objectStorageService.metadata(attachment.id) returns Some(ObjectStorageService.Metadata(backingFile.length(), "application/octet-stream", None))
		attachment.objectStorageService.fetch(attachment.id) answers { _ => Some(new FileInputStream(backingFile)) }

		submission.values = Set(SavedFormValue.withAttachments(submission, "files", Set(attachment))).asJava
		assignment.module = module
		val items = service.getSubmissionZipItems(submission)
		items.size should be (1)
		items.head.name should be ("ph105 - 0000007 - garble.doc")
	}




	@Test def readZip() {
		val zip = new ZipInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.map(zip){ _.getName }.sorted
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedböck.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}

	@Test def iterateZip() {
		val zip = new ZipArchiveInputStream(new ClassPathResource("/feedback1.zip").getInputStream)
		val names = Zips.iterator(zip){ (iterator) =>
			for (i <- iterator) yield i.getName
		}
		names should have ('size(8))
		names should contain("0123456/")
		names should contain("0123456/feedback.doc")
		names should contain("0123456/feedböck.mp3")
		names should contain("0123456/feedback.txt")
		names should contain("0123457/")
		names should contain("0123457/crayons.doc")
		names should contain("0123457/feedback.mp3")
		names should contain("marks.csv")
	}
}
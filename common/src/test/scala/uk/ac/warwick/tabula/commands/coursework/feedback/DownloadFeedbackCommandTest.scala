package uk.ac.warwick.tabula.commands.coursework.feedback

import com.google.common.io.Files
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.pdf.PdfGeneratorWithFileStorage
import uk.ac.warwick.tabula.services.objectstore.{ObjectStorageService, RichByteSource}
import uk.ac.warwick.tabula.services.{UserLookupService, ZipService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.concurrent.Future

class DownloadFeedbackCommandTest extends TestBase with Mockito {

  trait Fixture {
    val mockObjectStorageService: ObjectStorageService = smartMock[ObjectStorageService]

    var userDatabase = Seq(new User())
    var userLookup: UserLookupService = smartMock[UserLookupService]
    userLookup.getUserByUserId(any[String]) answers { id: Any =>
      userDatabase find {
        _.getUserId == id
      } getOrElse new AnonymousUser()
    }
    userLookup.getUserByWarwickUniId(any[String]) answers { id: Any =>
      userDatabase find {
        _.getWarwickId == id
      } getOrElse new AnonymousUser()
    }

    val department: Department = Fixtures.department(code = "ls", name = "Life Sciences")
    val module: Module = Fixtures.module(code = "ls101")
    val assignment = new Assignment
    val feedback: AssignmentFeedback = Fixtures.assignmentFeedback("0123456")
    feedback.id = "123"

    department.postLoad // force legacy settings
    module.adminDepartment = department
    assignment.module = module
    assignment.addFeedback(feedback)

    val attachment = new FileAttachment
    attachment.id = "123"
    attachment.objectStorageService = mockObjectStorageService
    attachment.objectStorageService.fetch(attachment.id) returns Future.successful(RichByteSource.wrap(Files.asByteSource(createTemporaryFile()), Some(ObjectStorageService.Metadata(contentLength = 0, contentType = "application/doc", fileHash = None))))
    attachment.name = "0123456-feedback.doc"
    feedback.attachments.add(attachment)

    val mockPdfGenerator: PdfGeneratorWithFileStorage = smartMock[PdfGeneratorWithFileStorage]
    mockPdfGenerator.renderTemplateAndStore(any[String], any[String], any[Any]) answers  { _: Any =>
      val attachment = new FileAttachment
      attachment.id = "234"
      attachment.objectStorageService = mockObjectStorageService
      attachment.objectStorageService.fetch(attachment.id) returns Future.successful(RichByteSource.wrap(Files.asByteSource(createTemporaryFile()), Some(ObjectStorageService.Metadata(contentLength = 0, contentType = "application/doc", fileHash = None))))
      attachment.name = "feedback.pdf"
      attachment
    }
  }

  @Test def applyCommand() {
    new Fixture {
      withUser("custard") {
        feedback.assignment.feedbacks.size should be(1)

        val command = new DownloadFeedbackCommand(feedback.assignment.module, feedback.assignment, feedback, Some(Fixtures.student("123")))
        command.zip = new ZipService {
          override val pdfGenerator: PdfGeneratorWithFileStorage = mockPdfGenerator
          override val fileDao: FileDao = smartMock[FileDao]
        }
        command.zip.userLookup = userLookup
        command.zip.features = Features.empty
        command.zip.objectStorageService = createTransientObjectStore()

        command.filename = attachment.name

        val errors = new BindException(command, "command")
        withClue(errors) {
          errors.hasErrors should be (false)
        }

        command.applyInternal().get.filename should be(attachment.name)

        command.filename = null
        command.applyInternal().get.byteSource.openStream() should not be null
      }
    }
  }

}

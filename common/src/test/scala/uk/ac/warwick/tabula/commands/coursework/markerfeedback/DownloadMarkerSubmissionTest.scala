package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import java.util.zip.ZipInputStream

import com.google.common.io.ByteSource
import org.junit.Before
import uk.ac.warwick.tabula.commands.coursework.assignments.DownloadMarkersSubmissionsCommand
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.{Features, Mockito, TestBase}

import scala.collection.JavaConversions._

class DownloadMarkerSubmissionTest extends TestBase with MarkingWorkflowWorld with Mockito {

  @Before
  def setup() {
    val attachment = new FileAttachment
		attachment.id = "123"

		attachment.objectStorageService = zipService.objectStorageService
		attachment.objectStorageService.push(attachment.id, ByteSource.wrap("yes".getBytes), ObjectStorageService.Metadata(3, "application/octet-stream", None))

    assignment.submissions.foreach {
      submission =>
        submission.values.add({
          val sv = new SavedFormValue()
          sv.attachments = Set(attachment)
          sv
        })
    }
		assignment.markingWorkflow.userLookup = mockUserLookup
  }

	trait CommandTestSupport extends ZipServiceComponent with AssessmentServiceComponent with StateServiceComponent {
		val assessmentService: AssessmentService = smartMock[AssessmentService]
		val stateService: StateService = smartMock[StateService]
		val zipService = new ZipService
		zipService.userLookup = mockUserLookup
		zipService.features = Features.empty
		zipService.objectStorageService = createTransientObjectStore()
	}

  @Test
  def downloadSubmissionsTest() {
    withUser("cuslaj", "1111111") {
      val command = new DownloadMarkersSubmissionsCommand(assignment.module, assignment, currentUser.apparentUser, currentUser) with CommandTestSupport
			val zip = command.applyInternal()
      val stream = new ZipInputStream(zip.inputStream)
      val items = Zips.map(stream) {
        item => item.getName
      }
      items.size should be(3)
    }
  }

}

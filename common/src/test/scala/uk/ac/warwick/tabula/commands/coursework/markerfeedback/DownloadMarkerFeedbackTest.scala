package uk.ac.warwick.tabula.commands.coursework.markerfeedback

import java.util.zip.ZipInputStream

import com.google.common.io.ByteSource
import org.junit.Before
import uk.ac.warwick.tabula.commands.coursework.feedback.{AdminGetSingleMarkerFeedbackCommand, DownloadMarkersFeedbackForPositionCommand}
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.data.model.{FileAttachment, FirstFeedback, MarkerFeedback}
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, Zips}
import uk.ac.warwick.tabula.{Mockito, TestBase}

import collection.JavaConverters._

class DownloadMarkerFeedbackTest extends TestBase with MarkingWorkflowWorld with Mockito {

	@Before
	def setup() {
		val attachment = new FileAttachment
		attachment.id = "123"

		attachment.objectStorageService = zipService.objectStorageService
		attachment.objectStorageService.push(attachment.id, ByteSource.wrap("yes".getBytes), ObjectStorageService.Metadata(3, "application/octet-stream", None))

		assignment.feedbacks.asScala.foreach { feedback =>
			feedback.firstMarkerFeedback.attachments = List(attachment).asJava
			feedback.firstMarkerFeedback.state = MarkingCompleted
			val smFeedback = new MarkerFeedback(feedback)
			feedback.secondMarkerFeedback = smFeedback
			smFeedback.state = ReleasedForMarking
		}
	}

	@Test
	def downloadSingle() { withUser("cuslaj"){
		val markerFeedback = assignment.getMarkerFeedback("cusxad", currentUser.apparentUser, FirstFeedback)
		val command = new AdminGetSingleMarkerFeedbackCommand(assignment.module, assignment, markerFeedback.get)
		command.zipService = zipService
		val renderable = command.applyInternal()
		val stream = new ZipInputStream(renderable.inputStream)
		val items = Zips.map(stream){item => item.getName}
		items.size should be (1)
	}}

	@Test
	def downloadAll() { withUser("cuslat", "1171795"){
		val command = new DownloadMarkersFeedbackForPositionCommand(assignment.module, assignment, currentUser.apparentUser, currentUser, FirstFeedback) with AutowiringZipServiceComponent
		assignment.markingWorkflow.userLookup = mockUserLookup
		command.zipService = zipService
		val renderable = command.applyInternal()
		val stream = new ZipInputStream(renderable.inputStream)
		val items = Zips.map(stream){item => item.getName}
		items.size should be (3)
	}}

}

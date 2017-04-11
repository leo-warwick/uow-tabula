package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings.ConvertScheduledMeetingRecordController
import uk.ac.warwick.tabula.{Mockito, TestBase}

class CreateScheduledMeetingRecordControllerTest extends TestBase with Mockito {

	val controller = new ConvertScheduledMeetingRecordController

	// TAB-2430
	@Test
	def createCommand() {
		val cmd = controller.getCreateCommand(null)
		cmd should be(null)
	}
}

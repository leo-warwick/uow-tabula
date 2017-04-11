package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.commands.profiles.relationships.meetings.EditMeetingRecordCommandInternal
import uk.ac.warwick.tabula.data.model.{MeetingRecord, MemberStudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings._

class EditMeetingRecordControllerTest extends TestBase with Mockito{

	@Test
	def nullMeetingRecordIsAbsolutelyNotOK() {
		val controller = new EditMeetingRecordController()
		intercept[ItemNotFoundException] {
			controller.getCommand(null)
			fail
		}
	}

	@Test
	def returnsAppropriateCommand(){
		val controller = new EditMeetingRecordController()

		// faffy setup to keep the permissions checking happy
		val relationship = new MemberStudentRelationship
		relationship.relationshipType = new StudentRelationshipType
		relationship.studentMember = Fixtures.student()
		val meeting = new MeetingRecord()
		meeting.relationship = relationship
		// end setup

		controller.getCommand(meeting) should be(anInstanceOf[EditMeetingRecordCommandInternal])
		controller.getCommand(meeting) should not be null

	}
}

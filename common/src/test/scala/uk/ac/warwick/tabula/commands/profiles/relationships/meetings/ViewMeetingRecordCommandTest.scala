package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Mockito, TestBase}

class ViewMeetingRecordCommandTest extends TestBase with Mockito {

	@Test
	def listsAllMeetingsForRequestedStudentAndCurrentUser() {
		withUser("test"){
			val studentCourseDetails = new StudentCourseDetails()
			val requestor = new StaffMember()
			val relationship = new MemberStudentRelationship()
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			val meeting = new MeetingRecord
			val command = new ViewMeetingRecordCommandInternal(studentCourseDetails, Some(requestor), relationshipType)
				with RelationshipServiceComponent with ProfileServiceComponent with MeetingRecordServiceComponent {
				val profileService: ProfileService = mock[ProfileService]
				var relationshipService: RelationshipService = mock[RelationshipService]
				val meetingRecordService: MeetingRecordService = mock[MeetingRecordService]
			}

			// these are the calls we expect the applyInternal method to make
			command.relationshipService.getRelationships(relationshipType, studentCourseDetails) returns Seq(relationship)
			command.meetingRecordService.listAll(Set(relationship), Some(requestor)) returns  Seq(meeting)

			command.applyInternal() should be (Seq(meeting))
		}
	}

}

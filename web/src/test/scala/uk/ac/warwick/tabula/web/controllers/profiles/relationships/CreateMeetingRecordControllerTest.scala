package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings._

class CreateMeetingRecordControllerTest extends TestBase with Mockito {

	val student: StudentMember = Fixtures.student()
	val studentCourseDetails: StudentCourseDetails = student.mostSignificantCourseDetails.get

	val controller = new CreateMeetingRecordController

	val profileService: ProfileService = mock[ProfileService]
	controller.profileService = profileService
	val relationshipService: RelationshipService = mock[RelationshipService]
	controller.relationshipService = relationshipService

	val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

	when(profileService.getAllMembersWithUserId("tutor", true)).thenReturn(Seq())
	when(profileService.getAllMembersWithUserId("supervisor", true)).thenReturn(Seq())

	@Test(expected=classOf[ItemNotFoundException])
	def throwsWithoutRelationships() {
		withUser("tutor") {
			controller.getCommand(relationshipType, studentCourseDetails, null, Nil)
		}
	}

	@Test
	def passesRelationshipTypeToCommand() {
		withUser("tutor") {
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			val relationship = new MemberStudentRelationship
			relationship.studentMember = student
			relationship.relationshipType = relationshipType

			val tutorCommand = controller.getCommand(relationshipType, studentCourseDetails, relationship, Seq(relationship))
			tutorCommand.relationship.relationshipType should be(relationshipType)
		}
	}

	@Test
	def passesFirstRelationshipTypeToCommand() {
		val uniId = "1234765"
		withUser("supervisor", uniId) {
			val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

			val firstAgent = "first"
			// use non-numeric agent in test to avoid unecessary member lookup
			val rel1 = new ExternalStudentRelationship
			rel1.studentMember = student
			rel1.relationshipType = relationshipType
			rel1.agent = firstAgent

			val secondAgent = "second"
			val rel2 = new ExternalStudentRelationship
			rel2.studentMember = student
			rel2.relationshipType = relationshipType
			rel2.agent = secondAgent

			studentCourseDetails.relationshipService = relationshipService
			controller.profileService = profileService

			val supervisorCommand = controller.getCommand(relationshipType, studentCourseDetails, null, Seq(rel1, rel2))
			supervisorCommand.relationship.relationshipType should be(relationshipType)
			supervisorCommand.relationship.agent should be(firstAgent)
			supervisorCommand.creator.universityId should be(uniId)
		}
	}

}

package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services.ProfileService

class StudentRelationshipTest extends TestBase with Mockito {

	val profileService: ProfileService = mock[ProfileService]

	@Test def agentMember() {
		val relType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val staff = Fixtures.staff(universityId="0672089")
		staff.firstName = "Steve"
		staff.lastName = "Taff"

		val student = Fixtures.student(universityId="0205225")

		val rel = StudentRelationship(staff, relType, student, DateTime.now)
		rel.isAgentMember should be (true)

		rel.agentMember should be (Some(staff))
		rel.agent should be ("0672089")
		rel.agentName should be ("Steve Taff")
		rel.agentLastName should be ("Taff")

		rel.studentId should be ("0205225")

		rel.studentMember.get should be (student)
	}

	@Test def stringMethod() {
		val relType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		val staff = Fixtures.staff(universityId="0672089")
		staff.firstName = "Steve"
		staff.lastName = "Taff"

		val student = Fixtures.student(universityId="0205225")

		val rel = StudentRelationship(staff, relType, student, DateTime.now)
		rel.id = "hibernateid"
		rel.toString should be ("MemberStudentRelationship[hibernateid][agent=0672089,relationshipType=StudentRelationshipType(tutor),student=0205225]")
	}

}
package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.Fixtures

class StudentRelationshipPersistenceTest extends PersistenceTestBase {

	trait Fixture {
		val student: StudentMember = Fixtures.student(universityId = "1000001")
		val memberAgent: StaffMember = Fixtures.staff(universityId = "4387483")
		memberAgent.firstName = "Anne"
		memberAgent.lastName = "Frank"

		val externalAgent = "Professor A Frank"

		val relType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

		session.save(student)
		session.save(memberAgent)
		session.save(relType)
		session.flush()
	}

	@Test def memberRelationship() { transactional { tx => new Fixture {
		val rel = StudentRelationship(memberAgent, relType, student, DateTime.now)
		session.save(rel)
		session.flush()
		session.clear()

		val loadedRel: StudentRelationship = session.get(classOf[StudentRelationship], rel.id).asInstanceOf[StudentRelationship]
		loadedRel.isAgentMember should be (true)
		loadedRel.agent should be ("4387483")
		loadedRel.agentMember should be (Some(memberAgent))
		loadedRel.studentId should be ("1000001")
		loadedRel.studentMember should be (Some(student))
		loadedRel.relationshipType should be (relType)
		loadedRel.agentName should be ("Anne Frank")
		loadedRel.agentLastName should be ("Frank")
	}}}

	@Test def externalRelationship() { transactional { tx => new Fixture {
		val rel = ExternalStudentRelationship(externalAgent, relType, student, DateTime.now)
		session.save(rel)
		session.flush()
		session.clear()

		val loadedRel: StudentRelationship = session.get(classOf[StudentRelationship], rel.id).asInstanceOf[StudentRelationship]
		loadedRel.isAgentMember should be (false)
		loadedRel.agent should be (externalAgent)
		loadedRel.agentMember should be (None)
		loadedRel.studentId should be ("1000001")
		loadedRel.studentMember should be (Some(student))
		loadedRel.relationshipType should be (relType)
		loadedRel.agentName should be (externalAgent)
		loadedRel.agentLastName should be (externalAgent)
	}}}

}
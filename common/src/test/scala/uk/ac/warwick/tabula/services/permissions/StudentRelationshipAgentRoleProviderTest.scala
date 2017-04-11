package uk.ac.warwick.tabula.services.permissions

import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.roles.StudentRelationshipAgent
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}

trait StudentRelationshipRoleTestBase extends TestBase with Mockito {
	val provider : RoleProvider with RelationshipServiceComponent

	val student: StudentMember = Fixtures.student(universityId = "111111")
	val staff: StaffMember = Fixtures.staff(universityId = "0123456", userId = "cuslaj")
	val oldStaff: StaffMember = Fixtures.staff(universityId = "7891011", userId = "cusxad")

	val relService: RelationshipService = smartMock[RelationshipService]

	val personalTutor = StudentRelationshipType("1", "tutor", "personal tutor", "personal tutee")
	val rel = StudentRelationship(staff, personalTutor, student, DateTime.now)
	val oldRel = StudentRelationship(oldStaff, personalTutor, student, DateTime.now)
}


class StudentRelationshipAgentRoleProviderTest extends StudentRelationshipRoleTestBase {

	val provider = new StudentRelationshipAgentRoleProvider {
		this.relationshipService = relService
	}
	relService.getCurrentRelationships(student, staff.universityId) returns Seq(rel)
	relService.getCurrentRelationships(student, oldStaff.universityId) returns Nil
	relService.listAllStudentRelationshipsWithUniversityId("0123456") returns Nil

	@Test
	def agent() = withUser("cuslaj", "0123456") {
		provider.getRolesFor(currentUser, student).force should be (Seq(StudentRelationshipAgent(student, personalTutor)))
	}

	@Test
	def notAgent() = withUser("cusxad", "7891011") {
		provider.getRolesFor(currentUser, student) should be (Nil)
	}

	@Test
	def handlesDefault() = withUser("cuscav", "0123456") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IN202")) should be (Seq())
	}

}
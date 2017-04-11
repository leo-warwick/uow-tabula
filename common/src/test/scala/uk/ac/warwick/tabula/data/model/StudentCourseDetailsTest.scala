package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal

class StudentCourseDetailsTest extends PersistenceTestBase with Mockito {

	val relationshipService: RelationshipService = mock[RelationshipService]

	val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")

	val student = new StudentMember

	val studentCourseDetails = new StudentCourseDetails(student, "0205225/1")
	studentCourseDetails.sprCode = "0205225/1"
	studentCourseDetails.relationshipService = relationshipService

	student.attachStudentCourseDetails(studentCourseDetails)

	val staff: StaffMember = Fixtures.staff(universityId="0672089")
	staff.firstName = "Steve"
	staff.lastName = "Taff"

	@Test def personalTutor() {
		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails) returns Nil
		student.freshStudentCourseDetails.head.relationships(relationshipType) should be ('empty)

		val rel = StudentRelationship(staff, relationshipType, student, DateTime.now)

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails) returns Seq(rel)
		student.freshStudentCourseDetails.head.relationships(relationshipType) flatMap { _.agentMember } should be (Seq(staff))
	}

	@Test def testModuleRegistrations() {
		val member = new StudentMember
		member.universityId = "01234567"

		// create a student course details with module registrations
		val scd1 = new StudentCourseDetails(member, "2222222/2")
		member.attachStudentCourseDetails(scd1)

		val mod1 = new Module("cs101")
		val mod2 = new Module("cs102")
		val modReg1 = new ModuleRegistration(scd1, mod1, new JBigDecimal("12.0"), AcademicYear(2012), "A")
		val modReg2 = new ModuleRegistration(scd1, mod2, new JBigDecimal("12.0"), AcademicYear(2013), "A")

		scd1.addModuleRegistration(modReg1)
		scd1.addModuleRegistration(modReg2)

		scd1.registeredModulesByYear(Some(AcademicYear(2013))) should be (Seq(mod2))
		scd1.registeredModulesByYear(None) should be (Seq(mod1, mod2))

		scd1.moduleRegistrations should be (Seq(modReg1, modReg2))
		scd1.moduleRegistrationsByYear(Some(AcademicYear(2012))) should be (Seq(modReg1))

	}

	@Test def relationships() {
		val rel1 = StudentRelationship(staff, relationshipType, student, DateTime.now)
		rel1.id = "1"
		val rel2 = StudentRelationship(staff, relationshipType, student, DateTime.now)
		rel2.id = "2"

		relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails) returns Seq(rel1)

		rel1.studentCourseDetails = studentCourseDetails
		studentCourseDetails.relationships(relationshipType) should be (Seq(rel1))
	}

	@Test def relationshipsOfTypeOrderedByPercentage() {
		val rel1 = StudentRelationship(staff, relationshipType, student, DateTime.now)
		rel1.percentage = new JBigDecimal(40)
		val rel2 = StudentRelationship(staff, relationshipType, student, DateTime.now)
		val rel3 = StudentRelationship(staff, relationshipType, student, DateTime.now)
		rel3.percentage = new JBigDecimal(60)

		studentCourseDetails.allRelationships.add(rel1)
		studentCourseDetails.allRelationships.add(rel2)
		studentCourseDetails.allRelationships.add(rel3)

		studentCourseDetails.allRelationshipsOfType(relationshipType) should be (Seq(rel3, rel1, rel2))
	}

}

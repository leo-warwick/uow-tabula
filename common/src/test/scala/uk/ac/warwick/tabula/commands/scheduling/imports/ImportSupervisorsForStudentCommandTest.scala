package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.SupervisorImporter
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito}

class ImportSupervisorsForStudentCommandTest extends AppContextTestBase with Mockito with Logging {

	trait Environment extends ImportCommandFactoryForTesting {
		val scjCode = "1111111/1"
		val sprCode = "1111111/1"
		val uniId = "1111111"

		val relationshipType = StudentRelationshipType("supervisor", "supervisor", "supervisor", "supervisee")
		relationshipType.defaultSource = StudentRelationshipSource.SITS
		relationshipType.defaultRdxType = "SUP"
		session.saveOrUpdate(relationshipType)

		val department = new Department
		session.saveOrUpdate(department)

		// set up and persist student
		val supervisee = new StudentMember(uniId)
		supervisee.userId = "xxxxx"

		val course1 = new CourseFixture(scjCode, sprCode)

		session.saveOrUpdate(supervisee)

		val supervisor: StaffMember = newSupervisor("0070790", "cusdx")

		// create and persist supervisor
		def newSupervisor(uniId: String, userId: String): StaffMember = {
			val supervisorMember = new StaffMember(uniId)
			supervisorMember.userId = userId
			session.saveOrUpdate(supervisorMember)
			supervisorMember
		}

		// Asserts that this course details is current and has exactly one relationship with
		// the given supervisor member.
		def assertRelationshipIsValid(scd: StudentCourseDetails, expectedSupervisor: StaffMember) {
			val supRels = scd.relationships(relationshipType)
			withClue(s"should be 1 item in $supRels") { supRels.size should be (1) }
			val rel = supRels.head

			rel.endDate should be (null)
			rel.agent should be (expectedSupervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			(rel.percentage: BigDecimal) should be (100)
		}

		class CourseFixture (scjCode: String, sprCode: String) {

			val studentCourseDetails = new StudentCourseDetails(supervisee, scjCode)
			studentCourseDetails.sprCode = sprCode
			studentCourseDetails.department = department

			supervisee.attachStudentCourseDetails(studentCourseDetails)

			val route = new Route
			route.degreeType = Postgraduate
			studentCourseDetails.currentRoute = route
			session.saveOrUpdate(route)
		}

	}

	@Transactional
	@Test def testImportConcurrentCourseSupervisors() {

		new Environment {
			val course2 = new CourseFixture("1111111/2", "1111111/2")
			session.save(course2.studentCourseDetails)

			val supervisor2: StaffMember = newSupervisor("1273455","cusmbg")
		}

	}

	@Transactional
	@Test def testCaptureValidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			val codes = Seq((supervisor.universityId, new JBigDecimal("100")))
			val importer: SupervisorImporter = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes

			// test command
			val command = new ImportSupervisorsForStudentCommand(course1.studentCourseDetails)
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels: Seq[StudentRelationship] = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel: StudentRelationship = supRels.head

			rel.agent should be (supervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			(rel.percentage: BigDecimal) should be (100)
		}
	}

	@Transactional
	@Test def testCaptureInvalidSupervisor() {
		new Environment {
			// set up importer to return supervisor
			val importer: SupervisorImporter = smartMock[SupervisorImporter]

			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns Seq()

			// test command
			val command = new ImportSupervisorsForStudentCommand(course1.studentCourseDetails)
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels: Seq[StudentRelationship] = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (0)
		}
	}

	@Transactional
	@Test def testCaptureExistingOtherSupervisor() {
		new Environment {
			// create and persist existing supervisor
			val existingSupervisorMember = new StaffMember("1234")
			existingSupervisorMember.userId = "cusfal"
			session.saveOrUpdate(existingSupervisorMember)

			// create and persist existing relationship
			val existingRelationship = StudentRelationship(existingSupervisorMember, relationshipType, supervisee, DateTime.now)
			existingRelationship.startDate = new DateTime
			session.saveOrUpdate(existingRelationship)

			// set up importer to return supervisor
			val codes = Seq((supervisor.universityId, null))
			val importer: SupervisorImporter = smartMock[SupervisorImporter]
			importer.getSupervisorUniversityIds(scjCode, relationshipType) returns codes


			// test command
			val command = new ImportSupervisorsForStudentCommand(course1.studentCourseDetails)
			command.studentCourseDetails = course1.studentCourseDetails
			command.supervisorImporter = importer
			command.applyInternal()

			// check results
			val supRels: Seq[StudentRelationship] = supervisee.freshStudentCourseDetails.head.relationships(relationshipType)
			supRels.size should be (1)
			val rel: StudentRelationship = supRels.head

			rel.agent should be (supervisor.universityId)
			rel.studentMember should be (Some(supervisee))
			rel.relationshipType should be (relationshipType)
			rel.percentage should be (null)
		}
	}
}

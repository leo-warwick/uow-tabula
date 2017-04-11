package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ExpireRelationshipsOnOldCoursesCommandTest extends TestBase with Mockito {

	val tutorRelationshipType = new StudentRelationshipType
	tutorRelationshipType.id = "tutor"
	val agent: StaffMember = Fixtures.staff("234")
	val thisStudent: StudentMember = Fixtures.student("123")

	trait TestSupport extends RelationshipServiceComponent with ExpireRelationshipsOnOldCoursesCommandState {
		override val student: StudentMember = thisStudent
		override val relationshipService: RelationshipService = smartMock[RelationshipService]
		relationshipService.allStudentRelationshipTypes returns Seq(tutorRelationshipType)
		relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns Option(tutorRelationshipType)
	}

	trait Fixture {
		def testObject: TestSupport
	}

	trait StudentWithOneCurrentOneEndedCourse extends Fixture {
		val endedCourse: StudentCourseDetails = thisStudent.freshStudentCourseDetails.head
		endedCourse.endDate = DateTime.now.minusDays(1).toLocalDate
		val currentCourse: StudentCourseDetails = Fixtures.studentCourseDetails(thisStudent, null)
		thisStudent.attachStudentCourseDetails(currentCourse)
		val relationshipOnCurrentCourse = StudentRelationship(agent, tutorRelationshipType, currentCourse, DateTime.now.minusDays(7))
		val relationshipOnEndedCourse = StudentRelationship(agent, tutorRelationshipType, endedCourse, DateTime.now.minusDays(7))
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq(relationshipOnCurrentCourse, relationshipOnEndedCourse)
	}

	trait StudentWithOnlyEndedCourse extends Fixture {
		val endedCourse: StudentCourseDetails = thisStudent.freshStudentCourseDetails.head
		endedCourse.endDate = DateTime.now.minusMonths(12).toLocalDate
		val currentCourse: StudentCourseDetails = Fixtures.studentCourseDetails(thisStudent, null)
		currentCourse.endDate = DateTime.now.minusMonths(1).toLocalDate
		thisStudent.attachStudentCourseDetails(currentCourse)
		val relationshipOnCurrentCourse = StudentRelationship(agent, tutorRelationshipType, currentCourse, DateTime.now.minusMonths(1))
		val relationshipOnEndedCourse = StudentRelationship(agent, tutorRelationshipType, endedCourse, DateTime.now.minusMonths(12))
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq(relationshipOnCurrentCourse, relationshipOnEndedCourse)
	}


	trait ValidationFixture extends Fixture {
		val validator = new ExpireRelationshipsOnOldCoursesValidation with TestSupport
		val errors = new BindException(validator, "command")
		override val testObject: ExpireRelationshipsOnOldCoursesValidation with TestSupport = validator
	}

	@Test
	def validateNoOldCourses(): Unit = new ValidationFixture {
		thisStudent.freshStudentCourseDetails.head.endDate = null
		validator.validate(errors)
		errors.hasErrors should be {true}
	}

	@Test
	def validateNoCurrentRelationships(): Unit = new ValidationFixture {
		thisStudent.freshStudentCourseDetails.head.endDate = DateTime.now.minusDays(1).toLocalDate
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq()
		validator.validate(errors)
		errors.hasErrors should be {true}
	}

	@Test
	def validateAlreadyExpired(): Unit = new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		relationshipOnEndedCourse.endDate = DateTime.now.minusDays(1)
		validator.validate(errors)
		errors.hasErrors should be {true}
	}

	@Test
	def validateHasExpired(): Unit = new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		validator.validate(errors)
		errors.hasErrors should be {false}
	}

	@Test
	def noCurrentRelationshipAndNotPastGracePeriod(): Unit = new ValidationFixture with StudentWithOnlyEndedCourse {
		validator.validate(errors)
		errors.hasErrors should be {true}
	}

	@Test
	def noCurrentRelationshipButPastGracePeriod(): Unit = new ValidationFixture with StudentWithOnlyEndedCourse {
		currentCourse.endDate = DateTime.now.minusMonths(3).toLocalDate
		validator.validate(errors)
		errors.hasErrors should be {false}
	}

	trait ApplyFixture extends Fixture {
		val command = new ExpireRelationshipsOnOldCoursesCommandInternal(thisStudent) with TestSupport
		override val testObject: ExpireRelationshipsOnOldCoursesCommandInternal with TestSupport = command
	}

	@Test
	def apply(): Unit = withFakeTime(DateTime.now) { new ApplyFixture with StudentWithOneCurrentOneEndedCourse {
		command.applyInternal()
		verify(command.relationshipService, times(1)).endStudentRelationships(Seq(relationshipOnEndedCourse), DateTime.now)
	}}

	@Test
	def applyAlreadyExpired(): Unit = withFakeTime(DateTime.now) { new ApplyFixture with StudentWithOneCurrentOneEndedCourse {
		relationshipOnEndedCourse.endDate = DateTime.now.minusDays(1)
		command.applyInternal()
		verify(command.relationshipService, times(1)).endStudentRelationships(Seq(), DateTime.now)
	}}

}

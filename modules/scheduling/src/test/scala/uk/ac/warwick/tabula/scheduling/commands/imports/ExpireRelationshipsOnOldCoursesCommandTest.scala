package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ExpireRelationshipsOnOldCoursesCommandTest extends TestBase with Mockito {

	val tutorRelationshipType = new StudentRelationshipType
	tutorRelationshipType.id = "tutor"
	val agent = Fixtures.staff("234")
	val thisStudent = Fixtures.student("123")

	trait TestSupport extends RelationshipServiceComponent with ExpireRelationshipsOnOldCoursesCommandState {
		override val student = thisStudent
		override val relationshipService = smartMock[RelationshipService]
		relationshipService.allStudentRelationshipTypes returns Seq(tutorRelationshipType)
		relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns Option(tutorRelationshipType)
	}
	
	trait Fixture {
		def testObject: TestSupport
	}

	trait StudentWithOneCurrentOneEndedCourse extends Fixture {
		val endedCourse = thisStudent.freshStudentCourseDetails.head
		endedCourse.endDate = DateTime.now.minusDays(1).toLocalDate
		val currentCourse = Fixtures.studentCourseDetails(thisStudent, null)
		thisStudent.attachStudentCourseDetails(currentCourse)
		val relationshipOnCurrentCourse = StudentRelationship(agent, tutorRelationshipType, currentCourse)
		val relationshipOnEndedCourse = StudentRelationship(agent, tutorRelationshipType, endedCourse)
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq(relationshipOnCurrentCourse, relationshipOnEndedCourse)
	}

	trait StudentWithOnlyEndedCourse extends Fixture {
		val endedCourse = thisStudent.freshStudentCourseDetails.head
		endedCourse.endDate = DateTime.now.minusMonths(12).toLocalDate
		val currentCourse = Fixtures.studentCourseDetails(thisStudent, null)
		currentCourse.endDate = DateTime.now.minusMonths(1).toLocalDate
		thisStudent.attachStudentCourseDetails(currentCourse)
		val relationshipOnCurrentCourse = StudentRelationship(agent, tutorRelationshipType, currentCourse)
		val relationshipOnEndedCourse = StudentRelationship(agent, tutorRelationshipType, endedCourse)
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq(relationshipOnCurrentCourse, relationshipOnEndedCourse)
	}


	trait ValidationFixture extends Fixture {
		val validator = new ExpireRelationshipsOnOldCoursesValidation with TestSupport
		val errors = new BindException(validator, "command")
		override val testObject = validator
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
		override val testObject = command
	}
	
	@Test
	def apply(): Unit = new ApplyFixture with StudentWithOneCurrentOneEndedCourse {
		command.applyInternal()
		verify(command.relationshipService, times(1)).endStudentRelationships(Seq(relationshipOnEndedCourse))
	}

	@Test
	def applyAlreadyExpired(): Unit = new ApplyFixture with StudentWithOneCurrentOneEndedCourse {
		relationshipOnEndedCourse.endDate = DateTime.now.minusDays(1)
		command.applyInternal()
		verify(command.relationshipService, times(1)).endStudentRelationships(Seq())
	}

}

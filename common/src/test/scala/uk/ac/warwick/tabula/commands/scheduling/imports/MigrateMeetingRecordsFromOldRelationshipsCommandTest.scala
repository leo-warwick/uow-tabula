package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{MeetingRecordService, MeetingRecordServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class MigrateMeetingRecordsFromOldRelationshipsCommandTest extends TestBase with Mockito {

	val tutorRelationshipType = new StudentRelationshipType
	tutorRelationshipType.id = "tutor"
	val agent: StaffMember = Fixtures.staff("234")
	val thisStudent: StudentMember = Fixtures.student("123", "123", null, Fixtures.department("xxx"))

	trait TestSupport extends RelationshipServiceComponent with MeetingRecordServiceComponent
		with MigrateMeetingRecordsFromOldRelationshipsCommandState {

		override val student: StudentMember = thisStudent
		override val relationshipService: RelationshipService = smartMock[RelationshipService]
		override val meetingRecordService: MeetingRecordService = smartMock[MeetingRecordService]

		relationshipService.getStudentRelationshipTypeByUrlPart("tutor") returns Option(tutorRelationshipType)
	}

	trait Fixture {
		def testObject: TestSupport
	}

	trait StudentWithOneCurrentOneEndedCourse extends Fixture {
		val endedCourse: StudentCourseDetails = thisStudent.freshStudentCourseDetails.head
		endedCourse.endDate = DateTime.now.minusDays(1).toLocalDate
		endedCourse.sprCode = "spr"
		val currentCourse: StudentCourseDetails = Fixtures.studentCourseDetails(thisStudent, endedCourse.department)
		currentCourse.sprCode = "spr"
		thisStudent.attachStudentCourseDetails(currentCourse)
		val relationshipOnCurrentCourse = StudentRelationship(agent, tutorRelationshipType, currentCourse, DateTime.now.minusDays(7))
		val relationshipOnEndedCourse = StudentRelationship(agent, tutorRelationshipType, endedCourse, DateTime.now.minusDays(7))
		relationshipOnEndedCourse.endDate = DateTime.now.minusDays(1)
		testObject.relationshipService.getRelationships(tutorRelationshipType, thisStudent) returns Seq(relationshipOnCurrentCourse, relationshipOnEndedCourse)
		val relationshipOnEndedCourseMeeting = new MeetingRecord
		relationshipOnEndedCourseMeeting.relationship = relationshipOnEndedCourse
		testObject.meetingRecordService.listAll(relationshipOnEndedCourse) returns Seq(relationshipOnEndedCourseMeeting)
	}

	trait ValidationFixture extends Fixture {
		val validator = new MigrateMeetingRecordsFromOldRelationshipsValidation with TestSupport
		val errors = new BindException(validator, "command")
		override val testObject: MigrateMeetingRecordsFromOldRelationshipsValidation with TestSupport = validator
	}

	@Test
	def validateNoMeetingRecords(): Unit = withFakeTime(DateTime.now) { new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		testObject.meetingRecordService.listAll(relationshipOnEndedCourse) returns Seq()
		validator.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def validateNoCorrespondingNotSpr(): Unit = withFakeTime(DateTime.now) { new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		endedCourse.sprCode = "somethingElse"
		validator.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def validateNoCorrespondingNotDepartment(): Unit = withFakeTime(DateTime.now) { new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		currentCourse.latestStudentCourseYearDetails.enrolmentDepartment = Fixtures.department("its")
		validator.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def validate(): Unit = withFakeTime(DateTime.now) { new ValidationFixture with StudentWithOneCurrentOneEndedCourse {
		validator.validate(errors)
		errors.hasErrors should be {false}
		testObject.migrations should be (Map(relationshipOnEndedCourse -> relationshipOnCurrentCourse))
	}}

	trait ApplyFixture extends Fixture {
		val command = new MigrateMeetingRecordsFromOldRelationshipsCommandInternal(thisStudent) with TestSupport
		override val testObject: MigrateMeetingRecordsFromOldRelationshipsCommandInternal with TestSupport = command
	}

	@Test
	def apply(): Unit = withFakeTime(DateTime.now) { new ApplyFixture with StudentWithOneCurrentOneEndedCourse {
		command.applyInternal()
		verify(command.meetingRecordService, times(1)).migrate(relationshipOnEndedCourse, relationshipOnCurrentCourse)
	}}

}

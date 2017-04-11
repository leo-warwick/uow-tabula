package uk.ac.warwick.tabula.commands.attendance.note

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{AbsenceType, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.userlookup.User

class EditAttendanceNoteCommandTest extends TestBase with Mockito {

	trait Fixture {

		val anAbsenceType = AbsenceType.Cancelled
		val theNote = "a note!"

		val command = new EditAttendanceNoteCommand(null, null, null, Option(""))
			with AttendanceMonitoringServiceComponent
			with FileAttachmentServiceComponent
			with UserLookupComponent
			with AttendanceNoteCommandState {
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			val fileAttachmentService = null
			val userLookup = null
			attendanceNote = new AttendanceMonitoringNote
			attendanceNote.student = student
			attendanceNote.point = point
			note = theNote
			absenceType = anAbsenceType
			override val user = new CurrentUser(new User, new User)
		}
		command.file.maintenanceMode = smartMock[MaintenanceModeService]

		val errors = new BindException(command, "command")

		val validator = new AttendanceNoteValidation with AttendanceNoteCommandState {
			val student: StudentMember = Fixtures.student("0000001", "student1")
			val point = null
		}
	}

	@Test
	def onBindNoExistingCheckpoints() { new Fixture {
		command.attendanceMonitoringService.getAttendanceNote(command.student, command.point) returns Option(command.attendanceNote)
		command.attendanceMonitoringService.getCheckpoints(Seq(command.point), command.student) returns Map()
		command.onBind(errors)
		command.checkpoint should be (null)
	}}

	@Test
	def onBindExistingCheckpoints() { new Fixture {
		val aCheckpoint = new AttendanceMonitoringCheckpoint
		command.attendanceMonitoringService.getAttendanceNote(command.student, command.point) returns Option(command.attendanceNote)
		command.attendanceMonitoringService.getCheckpoints(Seq(command.point), command.student) returns Map(command.point -> aCheckpoint)
		command.onBind(errors)
		command.checkpoint should be (aCheckpoint)
	}}

	@Test
	def testApply() { new Fixture {
		val attendanceNote: AttendanceMonitoringNote = command.applyInternal()
		attendanceNote.note should be (theNote)
		attendanceNote.absenceType should be(anAbsenceType)
		verify(command.attendanceMonitoringService, times(1)).saveOrUpdate(attendanceNote)
	}}

	@Test
	def validateNullAbsenceType() { new Fixture {
		validator.absenceType = null
		validator.validate(errors)
		errors.hasFieldErrors("absenceType") should be {true}
	}}

	@Test
	def validateValidAbsenceType() { new Fixture {
		validator.absenceType = AbsenceType.Academic
		validator.validate(errors)
		errors.hasFieldErrors("absenceType") should be {false}
	}}

}

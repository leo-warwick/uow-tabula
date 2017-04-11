package uk.ac.warwick.tabula.commands.attendance.view

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

class ReportStudentsChoosePeriodCommandTest extends TestBase with Mockito {

	val thisTermService: TermService = smartMock[TermService]

	trait TestSupport extends TermServiceComponent with AttendanceMonitoringServiceComponent with ProfileServiceComponent {
		val termService: TermService = thisTermService
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]
	}

	trait Fixture {
		val student1: StudentMember = Fixtures.student("1234", "1234")
		student1.lastName = "Smith"
		student1.firstName = "Fred"
		val student2: StudentMember = Fixtures.student("2345", "2345")
		student2.lastName = "Smith"
		student2.firstName = "Bob"

		val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(null)
		point1.startDate = new LocalDate(2014, 1, 1)
		val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(null)
		point2.startDate = new LocalDate(2014, 6, 6)

		val autumnTerm = new TermImpl(null, DateTime.now, null, TermType.autumn)
		val springTerm = new TermImpl(null, DateTime.now, null, TermType.spring)
		val christmasVacation = Vacation(autumnTerm, null)

		val fakeNow: DateTime = new LocalDate(2015, 1, 1).toDateTimeAtStartOfDay

		val state = new ReportStudentsChoosePeriodCommandState with TestSupport {
			val department: Department = Fixtures.department("its")
			val academicYear = AcademicYear(2014)
		}

		val validator = new ReportStudentsChoosePeriodValidation with ReportStudentsChoosePeriodCommandState with TestSupport {
			val department: Department = Fixtures.department("its")
			val academicYear = AcademicYear(2014)
		}

		thisTermService.getPreviousTerm(christmasVacation) returns autumnTerm
		thisTermService.getPreviousTerm(springTerm) returns christmasVacation

		val command = new ReportStudentsChoosePeriodCommandInternal(Fixtures.department("its"),  AcademicYear(2014)) with ReportStudentsChoosePeriodCommandState with TestSupport
	}

	@Test
	def allStudents() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()
		state.allStudents
		verify(state.profileService, times(1)).findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]])
	}}

	@Test
	def studentPointMap() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		val result: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = state.studentPointMap
		result(student1) should be (Seq(point1, point2))
		result(student2) should be (Seq(point1, point2))
	}}

	@Test
	def termPoints() { new Fixture {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		state.termService.getTermFromDateIncludingVacations(point2.startDate.toDateTimeAtStartOfDay) returns christmasVacation
		val result: Map[String, Seq[AttendanceMonitoringPoint]] = state.termPoints
		result(autumnTerm.getTermTypeAsString) should be (Seq(point1))
		result(christmasVacation.getTermTypeAsString) should be (Seq(point2))
	}}

	@Test
	def studentReportCounts() { new Fixture {
		state.period = autumnTerm.getTermTypeAsString
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1)
		state.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(autumnTerm.getTermTypeAsString)
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()

		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student2point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student2, AttendanceState.MissedUnauthorised)
		state.attendanceMonitoringService.getCheckpoints(Seq(point1), state.allStudents) returns
			Map(student1 -> Map(point1 -> student1point1missed),
			    student2 -> Map(point1 -> student2point1missed))

		val result: Seq[StudentReportCount] = state.studentReportCounts
		result.size should be (2)
	}}

	@Test
	def studentReportCountsUnreportedOnly() { new Fixture {
		state.period = autumnTerm.getTermTypeAsString
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1)
		state.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(autumnTerm.getTermTypeAsString)
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq()

		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student2point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student2, AttendanceState.MissedUnauthorised)
		state.attendanceMonitoringService.getCheckpoints(Seq(point1), state.allStudents) returns
			Map(student1 -> Map(point1 -> student1point1missed),
				student2 -> Map(point1 -> student2point1missed))

		state.attendanceMonitoringService.studentAlreadyReportedThisTerm(student1, point1) returns true
		state.attendanceMonitoringService.studentAlreadyReportedThisTerm(student2, point1) returns false

		val result: Seq[StudentReportCount] = state.studentMissedReportCounts
		result.size should be (1)
		result.head.student should be (student2)
	}}

	@Test
	def availablePeriods() { new Fixture { withFakeTime(fakeNow) {
		state.profileService.findAllStudentsByRestrictions(Matchers.eq(state.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		state.attendanceMonitoringService.listStudentsPoints(student1, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, Option(state.department), state.academicYear) returns Seq(point1, point2)
		state.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		state.termService.getTermFromDateIncludingVacations(point2.startDate.toDateTimeAtStartOfDay) returns christmasVacation
		state.termService.getTermFromDateIncludingVacations(fakeNow) returns springTerm
		state.attendanceMonitoringService.findNonReportedTerms(state.allStudents, state.academicYear) returns Seq(autumnTerm.getTermTypeAsString)
		val result = state.availablePeriods
		result.size should be (2)
		result.contains((autumnTerm.getTermTypeAsString, true)) should be {true}
		result.contains((autumnTerm.getTermTypeAsString, false)) should be {false}
		result.contains((christmasVacation.getTermTypeAsString, false)) should be {true}
		result.contains((christmasVacation.getTermTypeAsString, true)) should be {false}
	}}}

	trait ValidatorFixture extends Fixture {
		validator.profileService.findAllStudentsByRestrictions(Matchers.eq(validator.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		validator.attendanceMonitoringService.listStudentsPoints(student1, Option(validator.department), validator.academicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.listStudentsPoints(student2, Option(validator.department), validator.academicYear) returns Seq(point1, point2)
		validator.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		validator.termService.getTermFromDateIncludingVacations(point2.startDate.toDateTimeAtStartOfDay) returns christmasVacation
		validator.termService.getTermFromDateIncludingVacations(fakeNow) returns springTerm
		validator.attendanceMonitoringService.findNonReportedTerms(validator.allStudents, validator.academicYear) returns Seq(autumnTerm.getTermTypeAsString)
	}

	@Test
	def validation() {
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = springTerm.getTermTypeAsString
			validator.validate(errors)
			errors.getAllErrors.size should be (1)
		}}
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = christmasVacation.getTermTypeAsString
			validator.validate(errors)
			errors.getAllErrors.size should be (1)
		}}
		new ValidatorFixture { withFakeTime(fakeNow) {
			val errors = new BindException(validator, "command")
			validator.period = autumnTerm.getTermTypeAsString
			validator.validate(errors)
			errors.getAllErrors.size should be (0)
		}}
	}

	trait CommandFixture extends Fixture {
		command.profileService.findAllStudentsByRestrictions(Matchers.eq(command.department), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student1, student2)
		command.attendanceMonitoringService.listStudentsPoints(student1, Option(command.department), command.academicYear) returns Seq(point1, point2)
		command.attendanceMonitoringService.listStudentsPoints(student2, Option(command.department), command.academicYear) returns Seq(point1, point2)
		command.termService.getTermFromDateIncludingVacations(point1.startDate.toDateTimeAtStartOfDay) returns autumnTerm
		command.termService.getTermFromDateIncludingVacations(point2.startDate.toDateTimeAtStartOfDay) returns christmasVacation
		command.termService.getTermFromDateIncludingVacations(fakeNow) returns springTerm
		command.attendanceMonitoringService.findNonReportedTerms(command.allStudents, command.academicYear) returns Seq(autumnTerm.getTermTypeAsString)
	}

	@Test
	def apply() { new CommandFixture {
		val student1point1missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.MissedUnauthorised)
		val student1point2missed: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student1, AttendanceState.MissedUnauthorised)
		command.period = autumnTerm.getTermTypeAsString
		command.attendanceMonitoringService.getCheckpoints(Seq(point1), command.allStudents) returns Map(student1 -> Map(point1 -> student1point1missed, point2 -> student1point2missed))
		val result: Seq[StudentReportCount] = command.applyInternal()
		result.size should be (1)
		result.head.missed should be (1)
	}}
}

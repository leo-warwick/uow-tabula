package uk.ac.warwick.tabula.commands.attendance

import org.joda.time.DateTime
import org.joda.time.base.BaseDateTime
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert.AttendanceMonitoringPointIdConverter
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint, AttendanceMonitoringScheme, AttendanceState}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

class StudentRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisAcademicYear = AcademicYear(2014)
		val thisStudent: StudentMember = Fixtures.student("1234")

		val validator = new StudentRecordValidation with TermServiceComponent
			with AttendanceMonitoringServiceComponent with StudentRecordCommandState
			with StudentRecordCommandRequest {

			val termService: TermService = smartMock[TermService]
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			val academicYear: AcademicYear = thisAcademicYear
			val student: StudentMember = thisStudent
			val user = null
		}

		val scheme = new AttendanceMonitoringScheme
		val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "name1", 1, 1)
		point1.id = "1"
		validator.attendanceMonitoringService.getPointById(point1.id) returns Option(point1)
		val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "name2", 2, 2)
		point2.id = "2"
		validator.attendanceMonitoringService.getPointById(point2.id) returns Option(point2)
		val notInSchemePoint: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(null, "notInScheme", 1, 1)
		notInSchemePoint.id = "3"
		validator.attendanceMonitoringService.getPointById(notInSchemePoint.id) returns Option(notInSchemePoint)

		validator.attendanceMonitoringService.getCheckpoints(Seq(point1, point2), thisStudent, withFlush = false) returns Map()

		val autumnTerm = new TermImpl(null, null, null, TermType.autumn)

		val attendanceMonitoringPointConverter = new AttendanceMonitoringPointIdConverter
		attendanceMonitoringPointConverter.service = validator.attendanceMonitoringService
		val conversionService = new GenericConversionService()
		conversionService.addConverter(attendanceMonitoringPointConverter)

		var binder = new WebDataBinder(validator, "command")
		binder.setConversionService(conversionService)
		val errors: BindingResult = binder.getBindingResult

	}

	@Test
	def invalidPoint() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq()
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(notInSchemePoint, null)
		validator.validate(errors)

		errors.hasErrors should be {true}
		errors.hasFieldErrors(s"checkpointMap[${notInSchemePoint.id}]") should be {true}
	}}

	@Test
	def alreadyReported() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq()
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(point1, AttendanceState.Attended)
		validator.validate(errors)

		errors.hasErrors should be {true}
		errors.hasFieldErrors(s"checkpointMap[${point1.id}]") should be {true}
	}}

	@Test
	def tooSoon() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq()
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(point1, AttendanceState.Attended)
		validator.validate(errors)

		errors.hasErrors should be {true}
		errors.hasFieldErrors(s"checkpointMap[${point1.id}]") should be {true}
	}}

	@Test
	def beforeStartDateButNull() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq(autumnTerm.getTermTypeAsString)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(point1, null)
		validator.validate(errors)

		errors.hasErrors should be {false}
		errors.hasFieldErrors(s"checkpointMap[${point1.id}]") should be {false}
	}}

	@Test
	def beforeStartDateButAuthorised() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq(autumnTerm.getTermTypeAsString)
		validator.attendanceMonitoringService.getAttendanceNote(thisStudent, point1) returns Some(new AttendanceMonitoringNote)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(point1, AttendanceState.MissedAuthorised)
		validator.validate(errors)

		errors.hasErrors should be {false}
		errors.hasFieldErrors(s"checkpointMap[${point1.id}]") should be {false}
	}}

	@Test
	def authorisedWithNoNote() { new Fixture {
		validator.attendanceMonitoringService.listStudentsPoints(thisStudent, None, thisAcademicYear) returns Seq(point1, point2)
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(thisStudent), thisAcademicYear) returns Seq(autumnTerm.getTermTypeAsString)
		validator.attendanceMonitoringService.getAttendanceNote(thisStudent, point1) returns None
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm

		point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.checkpointMap = JHashMap()
		validator.checkpointMap.put(point1, AttendanceState.MissedAuthorised)
		validator.validate(errors)

		errors.hasErrors should be {true}
		errors.hasFieldErrors(s"checkpointMap[${point1.id}]") should be {true}

	}}

}

package uk.ac.warwick.tabula.commands.attendance.agent

import org.joda.time.DateTime
import org.joda.time.base.BaseDateTime
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.convert.AttendanceMonitoringPointIdConverter
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringPointStyle, AttendanceMonitoringScheme, AttendanceState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

import scala.collection.JavaConverters._
import scala.collection.mutable

class AgentPointRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val autumnTerm = new TermImpl(null, null, null, TermType.autumn)
		val dept1: Department = Fixtures.department("its1")
		val dept2: Department = Fixtures.department("its2")
		val thisRelationshipType = new StudentRelationshipType
		thisRelationshipType.id = "123"
		val student1: StudentMember = Fixtures.student("2345")
		val student1rel = new MemberStudentRelationship
		student1rel.studentMember = student1
		val student2: StudentMember = Fixtures.student("3456")
		val student2rel = new MemberStudentRelationship
		student2rel.studentMember = student2
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme1.department = dept1
		scheme1.academicYear = AcademicYear(2014)
		scheme1.members = UserGroup.ofUniversityIds
		scheme1.members.addUserId(student1.universityId)
		val scheme1point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, "point1", 1, 2)
		scheme1point1.id = "1234"
		val scheme1point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, "point2", 1, 2)
		scheme1point2.id = "2345"
		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme2.department = dept2
		scheme2.academicYear = AcademicYear(2014)
		scheme2.members = UserGroup.ofUniversityIds
		scheme2.members.addUserId(student1.universityId)
		scheme2.members.addUserId(student2.universityId)
		val scheme2point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme2, "point1", 1, 2)
		scheme2point1.id = "3456"
		val scheme2point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme2, "point2", 1, 2)
		scheme2point2.id = "4567"
		val currentUser = new CurrentUser(new User, new User)
	}

	trait StateTestSupport extends AttendanceMonitoringServiceComponent with TermServiceComponent with RelationshipServiceComponent {
		var relationshipType: StudentRelationshipType = null
		val academicYear = AcademicYear(2014)
		var templatePoint: AttendanceMonitoringPoint = null
		val user: CurrentUser = currentUser
		val member: StaffMember = Fixtures.staff("1234")
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val termService: TermService = smartMock[TermService]
		val relationshipService: RelationshipService = smartMock[RelationshipService]
	}

	trait StateFixture extends Fixture {
		val state = new AgentPointRecordCommandState with StateTestSupport
		state.relationshipType = thisRelationshipType
		state.relationshipService.listCurrentStudentRelationshipsWithMember(state.relationshipType, state.member) returns Seq(student1rel, student2rel)
		state.attendanceMonitoringService.listStudentsPoints(student1, None, state.academicYear) returns Seq(scheme1point1, scheme1point2, scheme2point1, scheme2point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, None, state.academicYear) returns Seq(scheme2point1, scheme2point2)
	}

	@Test
	def studentPointMap() {
		new StateFixture {
			state.templatePoint = scheme1point1
			val result: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = state.studentPointMap
			result(student1) should be (Seq(scheme1point1, scheme2point1))
			result(student2) should be (Seq(scheme2point1))
		}
	}

	trait PopulateTestSupport extends AgentPointRecordCommandState
		with AttendanceMonitoringServiceComponent with RelationshipServiceComponent with TermServiceComponent with SecurityServiceComponent {
			var relationshipType: StudentRelationshipType = null
			val academicYear = AcademicYear(2014)
			var templatePoint: AttendanceMonitoringPoint = null
			val user: CurrentUser = currentUser
			val member: StaffMember = Fixtures.staff("1234")
			val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
			val termService: TermService = smartMock[TermService]
			val relationshipService: RelationshipService = smartMock[RelationshipService]
			val securityService: SecurityService = smartMock[SecurityService]
	}

	trait PopulateFixture extends Fixture {
		val populate = new PopulateAgentPointRecordCommand with PopulateTestSupport
		populate.relationshipType = thisRelationshipType
		populate.relationshipService.listCurrentStudentRelationshipsWithMember(populate.relationshipType, populate.member) returns Seq(student1rel, student2rel)
		populate.attendanceMonitoringService.listStudentsPoints(student1, None, populate.academicYear) returns Seq(scheme1point1, scheme1point2, scheme2point1, scheme2point2)
		populate.attendanceMonitoringService.listStudentsPoints(student2, None, populate.academicYear) returns Seq(scheme2point1, scheme2point2)
		populate.attendanceMonitoringService.getCheckpoints(any[Seq[AttendanceMonitoringPoint]], any[Seq[StudentMember]]) returns Map(
			student2 -> Map(
				scheme2point1 -> Fixtures.attendanceMonitoringCheckpoint(scheme2point1, student2, AttendanceState.MissedAuthorised)
			)
		)
	}

	@Test
	def populate() {
		new PopulateFixture {
			populate.templatePoint = scheme1point1
			populate.populate()
			val result: collection.Map[StudentMember, mutable.Map[AttendanceMonitoringPoint, AttendanceState]] = populate.checkpointMap.asScala.mapValues(_.asScala)
			// Student1 doesn't have any attendance, but the checkpoint map should still be populated for each valid point
			result(student1)(scheme1point1) should be (null)
			result(student1)(scheme2point1) should be (null)
			result(student2)(scheme2point1) should be (AttendanceState.MissedAuthorised)
		}
	}

	trait ValidatorFixture extends Fixture {
		val validator = new AgentPointRecordValidation with AgentPointRecordCommandState with PopulateTestSupport

		validator.relationshipType = thisRelationshipType
		validator.relationshipService.listCurrentStudentRelationshipsWithMember(validator.relationshipType, validator.member) returns Seq(student1rel, student2rel)
		validator.attendanceMonitoringService.listStudentsPoints(student1, None, validator.academicYear) returns Seq(scheme1point1, scheme1point2, scheme2point1, scheme2point2)
		validator.attendanceMonitoringService.listStudentsPoints(student2, None, validator.academicYear) returns Seq(scheme2point1, scheme2point2)
		validator.attendanceMonitoringService.getCheckpoints(any[Seq[AttendanceMonitoringPoint]], any[Seq[StudentMember]]) returns Map(
			student2 -> Map(
				scheme2point1 -> Fixtures.attendanceMonitoringCheckpoint(scheme2point1, student2, AttendanceState.MissedAuthorised)
			)
		)
		validator.templatePoint = scheme2point1

		val conversionService = new GenericConversionService()

		val attendanceMonitoringPointConverter = new AttendanceMonitoringPointIdConverter
		attendanceMonitoringPointConverter.service = validator.attendanceMonitoringService
		conversionService.addConverter(attendanceMonitoringPointConverter)

		validator.attendanceMonitoringService.getPointById(scheme1point1.id) returns Option(scheme1point1)
		validator.attendanceMonitoringService.getPointById(scheme1point2.id) returns Option(scheme1point2)
		validator.attendanceMonitoringService.getPointById(scheme2point1.id) returns Option(scheme2point1)
		validator.attendanceMonitoringService.getPointById(scheme2point2.id) returns Option(scheme2point2)

		var binder = new WebDataBinder(validator, "command")
		binder.setConversionService(conversionService)
		val errors: BindingResult = binder.getBindingResult
	}

	@Test
	def validateInvalidPointNotMember() { new ValidatorFixture {
		validator.securityService.can(validator.user, Permissions.MonitoringPoints.Record, student2) returns {true}
		validator.checkpointMap = JHashMap(
			student2 -> JHashMap(scheme1point1 -> AttendanceState.MissedAuthorised.asInstanceOf[AttendanceState])
		)
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student2.universityId}][${scheme1point1.id}]") should be {true}
	}}

	@Test
	def validateInvalidPointNoPermission() { new ValidatorFixture {
		validator.securityService.can(validator.user, Permissions.MonitoringPoints.Record, student2) returns {false}
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(scheme1point1 -> AttendanceState.MissedAuthorised.asInstanceOf[AttendanceState])
		)
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${scheme1point1.id}]") should be {true}
	}}

	@Test
	def validateAlreadyReported() { new ValidatorFixture {
		validator.securityService.can(validator.user, Permissions.MonitoringPoints.Record, student1) returns {true}
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq()
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(scheme1point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${scheme1point1.id}]") should be {true}
	}}

	@Test
	def validateTooSoon() { new ValidatorFixture {
		validator.securityService.can(validator.user, Permissions.MonitoringPoints.Record, student1) returns {true}
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq(autumnTerm.getTermTypeAsString)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(scheme1point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		scheme1point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${scheme1point1.id}]") should be {true}
	}}

	@Test
	def validateOk() { new ValidatorFixture {
		validator.securityService.can(validator.user, Permissions.MonitoringPoints.Record, student1) returns {true}
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq(autumnTerm.getTermTypeAsString)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(scheme1point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		scheme1point1.startDate = DateTime.now.minusDays(2).toLocalDate
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${scheme1point1.id}]") should be {false}
	}}

}

package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember, UserGroup}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceMonitoringScheme, AttendanceState}
import uk.ac.warwick.tabula.commands.attendance.GroupedPoint
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.data.convert.{AttendanceMonitoringPointIdConverter, MemberUniversityIdConverter}
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.web.bind.WebDataBinder
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.util.termdates.TermImpl
import org.joda.time.base.BaseDateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.util.termdates.Term.TermType

class RecordMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandStateTestSupport extends AttendanceMonitoringServiceComponent
		with ProfileServiceComponent with TermServiceComponent with SecurityServiceComponent {

		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]
		val termService: TermService = smartMock[TermService]
		val securityService: SecurityService = smartMock[SecurityService]
	}

	trait Fixture {
		val thisUser = new User("cusfal")
		thisUser.setIsLoggedIn(true)
		thisUser.setFoundUser(true)

		val thisDepartment: Department = Fixtures.department("its")

		val student1: StudentMember = Fixtures.student("1234")
		val student2: StudentMember = Fixtures.student("2345")
		val student3: StudentMember = Fixtures.student("3456")

		val autumnTerm = new TermImpl(null, null, null, TermType.autumn)

		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.department = thisDepartment
		scheme1.academicYear = AcademicYear(2014)
		scheme1.members = UserGroup.ofUniversityIds
		scheme1.members.addUserId(student1.universityId)
		scheme1.members.addUserId(student2.universityId)
		val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1)
		point1.id = "123"
		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.department = thisDepartment
		scheme2.academicYear = AcademicYear(2014)
		scheme2.members = UserGroup.ofUniversityIds
		scheme2.members.addUserId(student2.universityId)
		scheme2.members.addUserId(student3.universityId)
		val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme2)
		point2.id = "234"

		val student1point1checkpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.Attended)
		val student2point1checkpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student2, AttendanceState.Attended)
		val student2point2checkpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student2, AttendanceState.Attended)
		val student3point2checkpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, student3, AttendanceState.Attended)
	}

	trait StateFixture extends Fixture {
		val state = new RecordMonitoringPointCommandState with CommandStateTestSupport {
			val templatePoint: AttendanceMonitoringPoint = point1
			val user: CurrentUser = new CurrentUser(thisUser, thisUser)
			val department: Department = thisDepartment
			val academicYear: AcademicYear = AcademicYear(2014)
		}
		state.profileService.getAllMembersWithUniversityIds(Seq(student1.universityId, student2.universityId)) returns Seq(student1, student2)
		state.profileService.getAllMembersWithUniversityIds(Seq(student2.universityId, student3.universityId)) returns Seq(student2, student3)
	}

	trait FilteredPointsFixture extends Fixture {
		val thisFilteredPoints =
			Map(
				"Autumn" -> Seq(GroupedPoint(point1, Seq(scheme1, scheme2), Seq(point1, point2))),
				"Spring" -> Seq(GroupedPoint(point2, Seq(), Seq()))
			)
	}

	trait StudentDatasFixture extends Fixture {
		val thisStudentDatas =
			Seq(
				AttendanceMonitoringStudentData(student1.firstName, student1.lastName, student1.universityId, "", new LocalDate, None, null, null, null, null),
				AttendanceMonitoringStudentData(student2.firstName, student2.lastName, student2.universityId, "", new LocalDate, None, null, null, null, null),
				AttendanceMonitoringStudentData(student3.firstName, student3.lastName, student3.universityId, "", new LocalDate, None, null, null, null, null)
			)
	}

	@Test
	def statePointsToRecord() {	new StateFixture with FilteredPointsFixture with StudentDatasFixture {
		state.filteredPoints = thisFilteredPoints
		state.studentDatas = thisStudentDatas
		val result: Seq[AttendanceMonitoringPoint] = state.pointsToRecord
		result.contains(point1) should be {true}
		result.contains(point2) should be {true}
	}}

	@Test
	def stateStudentMap() {	new StateFixture with FilteredPointsFixture with StudentDatasFixture {
		state.filteredPoints = thisFilteredPoints
		state.studentDatas = thisStudentDatas
		val result: Map[AttendanceMonitoringPoint, Seq[StudentMember]] = state.studentMap
		result(point1).contains(student1) should be {true}
		result(point1).contains(student2) should be {true}
		result(point1).contains(student3) should be {false}
		result(point2).contains(student1) should be {false}
		result(point2).contains(student2) should be {true}
		result(point2).contains(student3) should be {true}
	}}

	trait PopulateFixture extends Fixture {
		val populate = new PopulateRecordMonitoringPointCommand with RecordMonitoringPointCommandState with CommandStateTestSupport {
			val templatePoint: AttendanceMonitoringPoint = point1
			val user: CurrentUser = new CurrentUser(thisUser, thisUser)
			val department: Department = thisDepartment
			val academicYear: AcademicYear = AcademicYear(2014)
		}
		populate.profileService.getAllMembersWithUniversityIds(Seq(student1.universityId, student2.universityId)) returns Seq(student1, student2)
		populate.profileService.getAllMembersWithUniversityIds(Seq(student2.universityId, student3.universityId)) returns Seq(student2, student3)
		populate.attendanceMonitoringService.getCheckpoints(Seq(point1, point2), Seq(student1, student2, student3)) returns Map(
			student1 -> Map(point1 -> student1point1checkpoint),
			student2 -> Map(point1 -> student2point1checkpoint, point2 -> student2point2checkpoint),
			student3 -> Map(point2 -> student3point2checkpoint)
		)
	}

	@Test
	def populate() { new PopulateFixture with FilteredPointsFixture with StudentDatasFixture {
		populate.filteredPoints = thisFilteredPoints
		populate.studentDatas = thisStudentDatas
		populate.populate()
		val result: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]] = populate.checkpointMap.asScala.mapValues(_.asScala.toMap).toMap
		result(student1).keys.size should be (1)
		result(student2).keys.size should be (2)
		result(student3).keys.size should be (1)
	}}

	trait ValidatorFixture extends Fixture {
		val validator = new RecordMonitoringPointValidation with RecordMonitoringPointCommandState with CommandStateTestSupport {
			val templatePoint: AttendanceMonitoringPoint = point1
			val user: CurrentUser = new CurrentUser(thisUser, thisUser)
			val department: Department = thisDepartment
			val academicYear: AcademicYear = AcademicYear(2014)
		}

		validator.filteredPoints = Map("point" -> Seq(GroupedPoint(point1, Seq(), Seq(point1, point2))))
		validator.studentDatas = Seq(
			AttendanceMonitoringStudentData(null, null, student1.universityId, null, null, null, null, null, null, null),
			AttendanceMonitoringStudentData(null, null, student2.universityId, null, null, null, null, null, null, null),
			AttendanceMonitoringStudentData(null, null, student3.universityId, null, null, null, null, null, null, null)
		)

		val conversionService = new GenericConversionService()

		val attendanceMonitoringPointConverter = new AttendanceMonitoringPointIdConverter
		attendanceMonitoringPointConverter.service = validator.attendanceMonitoringService
		conversionService.addConverter(attendanceMonitoringPointConverter)

		validator.attendanceMonitoringService.getPointById(point1.id) returns Option(point1)
		validator.attendanceMonitoringService.getPointById(point2.id) returns Option(point2)
		validator.attendanceMonitoringService.getCheckpoints(Seq(point1, point2), Seq(student1, student2, student3)) returns Map()

		val memberUniversityIdConverter = new MemberUniversityIdConverter
		memberUniversityIdConverter.service = validator.profileService
		conversionService.addConverter(memberUniversityIdConverter)

		validator.profileService.getAllMembersWithUniversityIds(Seq(student1.universityId, student2.universityId)) returns Seq(student1, student2)
		validator.profileService.getAllMembersWithUniversityIds(Seq(student2.universityId, student3.universityId)) returns Seq(student2, student3)
		validator.profileService.getMemberByUniversityIdStaleOrFresh(student1.universityId) returns Option(student1)
		validator.profileService.getMemberByUniversityIdStaleOrFresh(student2.universityId) returns Option(student2)
		validator.profileService.getMemberByUniversityIdStaleOrFresh(student3.universityId) returns Option(student3)

		var binder = new WebDataBinder(validator, "command")
		binder.setConversionService(conversionService)
		val errors: BindingResult = binder.getBindingResult
	}

	@Test
	def validateInvalidPoint() { new ValidatorFixture {
		validator.checkpointMap = JHashMap(
			student3 -> JHashMap(point1 -> AttendanceState.MissedAuthorised.asInstanceOf[AttendanceState])
		)
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student3.universityId}][${point1.id}]") should be {true}
	}}

	@Test
	def validateAlreadyReported() { new ValidatorFixture {
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq()
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${point1.id}]") should be {true}
	}}

	@Test
	def validateTooSoon() { new ValidatorFixture {
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq(autumnTerm.getTermTypeAsString)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		point1.startDate = DateTime.now.plusDays(2).toLocalDate
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${point1.id}]") should be {true}
	}}

	@Test
	def validateOk() { new ValidatorFixture {
		validator.attendanceMonitoringService.findNonReportedTerms(Seq(student1), AcademicYear(2014)) returns Seq(autumnTerm.getTermTypeAsString)
		validator.termService.getTermFromDateIncludingVacations(any[BaseDateTime]) returns autumnTerm
		validator.checkpointMap = JHashMap(
			student1 -> JHashMap(point1 -> AttendanceState.Attended.asInstanceOf[AttendanceState])
		)
		point1.startDate = DateTime.now.minusDays(2).toLocalDate
		validator.validate(errors)
		errors.hasFieldErrors(s"checkpointMap[${student1.universityId}][${point1.id}]") should be {false}
	}}

}
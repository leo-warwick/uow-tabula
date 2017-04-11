package uk.ac.warwick.tabula.services.attendancemonitoring

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.{AttendanceMonitoringDao, AttendanceMonitoringDaoComponent}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent, UserGroupMembershipHelper, UserLookupComponent}
import uk.ac.warwick.tabula._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}

class AttendanceMonitoringServiceTest extends TestBase with Mockito {

	trait ServiceTestSupport extends AttendanceMonitoringDaoComponent with TermServiceComponent
		with UserLookupComponent with AttendanceMonitoringMembershipHelpers {
		val attendanceMonitoringDao: AttendanceMonitoringDao = smartMock[AttendanceMonitoringDao]
		val termService: TermService = smartMock[TermService]
		val userLookup = new MockUserLookup
		val membersHelper: UserGroupMembershipHelper[AttendanceMonitoringScheme] = smartMock[UserGroupMembershipHelper[AttendanceMonitoringScheme]]
	}

	trait CheckpointFixture {

		val currentAcademicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val service = new AbstractAttendanceMonitoringService with ServiceTestSupport

		val department: Department = Fixtures.department("it")

		val uniId1 = "1234"
		val member1: StudentMember = Fixtures.student(uniId1)
		val uniId2 = "2345"
		val member2: StudentMember = Fixtures.student(uniId2)

		val scheme = new AttendanceMonitoringScheme
		scheme.department = department
		scheme.academicYear = currentAcademicYear
		service.membersHelper.findBy(MemberOrUser(member1).asUser) returns Seq(scheme)
		service.membersHelper.findBy(MemberOrUser(member2).asUser) returns Seq(scheme)

		val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "point1", 2, 2, currentAcademicYear)
		val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "point2", 4, 4, currentAcademicYear)
		val point3: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme, "point3", 4, 4, currentAcademicYear)
		scheme.points = JArrayList(point1, point2, point3)
		val passedCheckpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point1, member1, AttendanceState.fromCode("attended"))
		val missedCheckpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point2, member1, AttendanceState.fromCode("unauthorised"))
		val authorisedCheckpoint: AttendanceMonitoringCheckpoint = Fixtures.attendanceMonitoringCheckpoint(point3, member1, AttendanceState.fromCode("authorised"))
	}

	@Test
	def setAttendance() { new CheckpointFixture { withUser("cusfal") {
		member1.mostSignificantCourse.beginDate = currentAcademicYear.dateInTermOne.minusMonths(1).toLocalDate

		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1, withFlush = true) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpointTotal(member1, Option(department), currentAcademicYear) returns None

		val result = service.setAttendance(
			member1,
			Map(point1 -> AttendanceState.Attended, point2 -> AttendanceState.Attended, point3 -> null),
			currentUser
		)
		result._1.size should be (1)
		result._1.head should be (missedCheckpoint)
		verify(service.attendanceMonitoringDao, times(1)).removeCheckpoints(Seq(authorisedCheckpoint))
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdateCheckpoints(Seq(missedCheckpoint))
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdate(any[AttendanceMonitoringCheckpointTotal])
	}}}

	@Test
	def updateCheckpointTotal() { new CheckpointFixture { withUser("cusfal") {
		member1.mostSignificantCourse.beginDate = currentAcademicYear.dateInTermOne.minusMonths(1).toLocalDate

		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpoints(Seq(point1, point2, point3), member1, withFlush = true) returns
			Map(point1 -> passedCheckpoint, point2 -> missedCheckpoint, point3 -> authorisedCheckpoint)
		service.attendanceMonitoringDao.getCheckpointTotal(member1, Option(department), currentAcademicYear) returns None

		val result = service.updateCheckpointTotal(
			member1,
			department,
			currentAcademicYear
		)
		verify(service.attendanceMonitoringDao, times(1)).saveOrUpdate(result)
		result.attended should be (1)
		result.unauthorised should be (1)
		result.authorised should be (1)
		result.unrecorded should be (0)
	}}}

}

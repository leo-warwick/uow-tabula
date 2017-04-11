package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{DeserializesFilter, TaskBenchmarking}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.{ScalaOrder, ScalaRestriction}
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula._

class UpdateAttendanceMonitoringSchemeMembershipCommandTest extends TestBase with Mockito {

	var deserializeFilterCalled = false

	trait CommandTestSupport extends FeaturesComponent with AttendanceMonitoringServiceComponent
		with DeserializesFilter	with UpdateAttendanceMonitoringSchemeMembershipCommandState with TaskBenchmarking {

		val features: FeaturesImpl = emptyFeatures
		val attendanceMonitoringService: AttendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val profileService: ProfileService = smartMock[ProfileService]
		profileService.getAllMembersWithUniversityIds(Seq()) returns Seq()
		def deserializeFilter(filterString: String): Unit = {
			deserializeFilterCalled = true
		}
	}

	trait Fixture {
		deserializeFilterCalled = false
		val cmd = new UpdateAttendanceMonitoringSchemeMembershipCommandInternal with CommandTestSupport
	}

	@Test
	def noSchemes() { new Fixture {
		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq()
		val schemes: Seq[AttendanceMonitoringScheme] = cmd.applyInternal()
		deserializeFilterCalled should be {false}
		schemes.isEmpty should be {true}
	}}

	@Test
	def sameStudentInMultipleSchemesInSameDept() { new Fixture {
		val dept: Department = Fixtures.department("its")
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.department = dept
		scheme1.academicYear = AcademicYear(2014)

		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.department = dept
		scheme2.academicYear = AcademicYear(2014)
		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq(scheme1, scheme2)

		val student: StudentMember = Fixtures.student("1234")
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)

		val schemes: Seq[AttendanceMonitoringScheme] = cmd.applyInternal()
		deserializeFilterCalled should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme1)
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme2)
		schemes.contains(scheme1) should be {true}
		schemes.contains(scheme2) should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), dept, AcademicYear(2014))
	}}

	@Test
	def sameStudentInMultipleSchemesInDifferentDept() { new Fixture {
		val dept1: Department = Fixtures.department("its")
		val dept2: Department = Fixtures.department("foo")
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.attendanceMonitoringService = None
		scheme1.department = dept1
		scheme1.academicYear = AcademicYear(2014)

		val scheme2 = new AttendanceMonitoringScheme
		scheme2.attendanceMonitoringService = None
		scheme2.department = dept2
		scheme2.academicYear = AcademicYear(2014)

		cmd.attendanceMonitoringService.listSchemesForMembershipUpdate returns Seq(scheme1, scheme2)

		val student: StudentMember = Fixtures.student("1234")
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept1), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(isEq(dept2), any[Seq[ScalaRestriction]], any[Seq[ScalaOrder]]) returns Seq(student.universityId)
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)

		val schemes: Seq[AttendanceMonitoringScheme] = cmd.applyInternal()
		deserializeFilterCalled should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme1)
		verify(cmd.attendanceMonitoringService, times(1)).saveOrUpdate(scheme2)
		schemes.contains(scheme1) should be {true}
		schemes.contains(scheme2) should be {true}
		verify(cmd.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), dept1, AcademicYear(2014))
		verify(cmd.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), dept2, AcademicYear(2014))
	}}

}

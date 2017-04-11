package uk.ac.warwick.tabula.services

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.Before
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, PersistenceTestBase}
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class ProfileServiceTest extends PersistenceTestBase with Mockito {

	var profileService: ProfileService = _

	@Before def setup(): Unit = transactional { tx =>
		val thisMemberDao = new AutowiringMemberDaoImpl
		thisMemberDao.sessionFactory = sessionFactory
		val thisStudentCourseDetailsDao = new StudentCourseDetailsDaoImpl
		thisStudentCourseDetailsDao.sessionFactory = sessionFactory
		profileService = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: AutowiringMemberDaoImpl = thisMemberDao
			val studentCourseDetailsDao: StudentCourseDetailsDaoImpl = thisStudentCourseDetailsDao
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}
	}

	@Test def crud() = transactional { tx =>
		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		val m2 = Fixtures.student(universityId = "0000002", userId="student")

		val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
		val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		profileService.save(m1)
		profileService.save(m2)

		profileService.getMemberByUniversityId("0000001") should be (Some(m1))
		profileService.getMemberByUniversityId("0000002") should be (Some(m2))
		profileService.getMemberByUniversityId("0000003") should be (Some(m3))
		profileService.getMemberByUniversityId("0000004") should be (Some(m4))
		profileService.getMemberByUniversityId("0000005") should be (None)

		profileService.getStudentBySprCode("0000001/2") should be (Some(m1))
		profileService.getStudentBySprCode("0000002/2") should be (Some(m2))
		profileService.getStudentBySprCode("0000003/2") should be (None)
		profileService.getStudentBySprCode("0000001/3") should be (None)

		session.enableFilter(Member.StudentsOnlyFilter)

		profileService.getMemberByUniversityId("0000003") should be (None)
		profileService.getMemberByUniversityId("0000004") should be (None)

		profileService.getAllMembersWithUserId("student", disableFilter = false) should be (Seq(m1, m2))
		profileService.getMemberByUser(new User("student"), disableFilter = false) should be (Some(m1))
		profileService.getAllMembersWithUserId("student", disableFilter = true) should be (Seq(m1, m2))
		profileService.getAllMembersWithUserId("staff1", disableFilter = false) should be (Seq())
		profileService.getMemberByUser(new User("staff1"), disableFilter = false) should be (None)
		profileService.getAllMembersWithUserId("staff1", disableFilter = true) should be (Seq(m3))
		profileService.getMemberByUser(new User("staff1"), disableFilter = true) should be (Some(m3))
		profileService.getAllMembersWithUserId("unknown", disableFilter = false) should be (Seq())
		profileService.getAllMembersWithUserId("unknown", disableFilter = true) should be (Seq())

		session.disableFilter(Member.StudentsOnlyFilter)

		profileService.getAllMembersWithUserId("staff1", disableFilter = false) should be (Seq(m3))
		profileService.getMemberByUser(new User("staff1"), disableFilter = false) should be (Some(m3))
	}

	@Test def listMembersUpdatedSince() = transactional { tx =>

		val dept1 = Fixtures.departmentWithId("in", "IT Services", "1")
		val dept2 = Fixtures.departmentWithId("po", "Politics", "2")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		session.flush()

		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		profileService.listMembersUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())
	}

	@Test def studentsByRouteForAcademicYear() = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: MemberDao = mock[MemberDao]
			val studentCourseDetailsDao: StudentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"

		val studentBothYears = Fixtures.student(universityId = "0000001")
		val courseDetailsBothYears = new StudentCourseDetails(studentBothYears, "studentInBothYears")
		courseDetailsBothYears.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012), studentCourseDetails = courseDetailsBothYears)
		)
		courseDetailsBothYears.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2013), studentCourseDetails = courseDetailsBothYears)
		)
		courseDetailsBothYears.statusOnRoute = new SitsStatus("C")
		courseDetailsBothYears.mostSignificant = true
		studentBothYears.attachStudentCourseDetails(courseDetailsBothYears)


		val studentFirstYear = Fixtures.student(universityId = "0000002")
		val courseDetailsFirstYear = new StudentCourseDetails(studentFirstYear, "studentInFirstYear")
		courseDetailsFirstYear.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012), studentCourseDetails = courseDetailsFirstYear)
		)
		courseDetailsFirstYear.statusOnRoute = new SitsStatus("C")
		courseDetailsFirstYear.mostSignificant = true
		studentFirstYear.attachStudentCourseDetails(courseDetailsFirstYear)


		val studentSecondYear = Fixtures.student(universityId = "0000003")
		val courseDetailsSecondYear = new StudentCourseDetails(studentSecondYear, "studentInSecondYear")
		courseDetailsSecondYear.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2013), studentCourseDetails = courseDetailsSecondYear)
		)
		courseDetailsSecondYear.statusOnRoute = new SitsStatus("C")
		courseDetailsSecondYear.mostSignificant = true
		studentSecondYear.attachStudentCourseDetails(courseDetailsSecondYear)

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(courseDetailsBothYears, courseDetailsFirstYear, courseDetailsSecondYear)

		val studentsInFirstYear = service.getStudentsByRoute(testRoute, AcademicYear(2012))
		studentsInFirstYear.size should be (2)
		studentsInFirstYear.exists(
			s => s.freshStudentCourseDetails.head.scjCode.equals(courseDetailsSecondYear.scjCode)
		) should be {false}

		val studentsInSecondYear = service.getStudentsByRoute(testRoute, AcademicYear(2013))
		studentsInSecondYear.size should be (2)
		studentsInSecondYear.exists(
			s => s.freshStudentCourseDetails.head.scjCode.equals(courseDetailsFirstYear.scjCode)
		) should be {false}
	}

	@Test def studentsByRouteWithdrawn() = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: MemberDao = mock[MemberDao]
			val studentCourseDetailsDao: StudentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"

		val studentNoShow = new StudentCourseDetails(Fixtures.student(), "studentNoShow")
		studentNoShow.statusOnRoute = new SitsStatus("PNS")
		studentNoShow.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawn = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawn")
		studentPermWithdrawn.statusOnRoute = new SitsStatus("P")
		studentPermWithdrawn.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawnDebt = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawnDebt")
		studentPermWithdrawnDebt.statusOnRoute = new SitsStatus("PD")
		studentPermWithdrawnDebt.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		val studentPermWithdrawnWrittenOff = new StudentCourseDetails(Fixtures.student(), "studentPermWithdrawnWrittenOff")
		studentPermWithdrawnWrittenOff.statusOnRoute = new SitsStatus("PR")
		studentPermWithdrawnWrittenOff.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(
			studentNoShow, studentPermWithdrawn, studentPermWithdrawnDebt, studentPermWithdrawnWrittenOff
		)

		val studentsWithYear = service.getStudentsByRoute(testRoute, AcademicYear(2012))
		studentsWithYear.size should be (0)

		val students = service.getStudentsByRoute(testRoute)
		students.size should be (0)
	}



	@Test def studentsByRouteMostSignificantCourse() = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: MemberDao = mock[MemberDao]
			val studentCourseDetailsDao: StudentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"
		val testYear = AcademicYear(2012)

		val student = new StudentCourseDetails(Fixtures.student(), "student")

		student.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		student.statusOnRoute = new SitsStatus("C")
		student.mostSignificant =  true

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(student)

		val students = service.getStudentsByRoute(testRoute, testYear)
		students.size should be (1)

		students.exists(
			s => s.freshStudentCourseDetails.head.mostSignificant.equals(true)
		) should be {true}

	}



	@Test def studentsByRouteNotMostSignificantCourse() = {
		val service = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: MemberDao = mock[MemberDao]
			val studentCourseDetailsDao: StudentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}

		val testRoute = new Route
		testRoute.code = "test"
		val testYear = AcademicYear(2012)

		val student = new StudentCourseDetails(Fixtures.student(), "student")

		student.addStudentCourseYearDetails(
			Fixtures.studentCourseYearDetails(AcademicYear(2012))
		)
		student.statusOnRoute = new SitsStatus("C")
		student.mostSignificant = false

		service.studentCourseDetailsDao.getByRoute(testRoute) returns Seq(student)

		val students = service.getStudentsByRoute(testRoute, testYear)
		students.size should be (0)

		students.exists(
			s => s.freshStudentCourseDetails.head.mostSignificant.equals(true)
		) should be {false}

	}
	trait MockFixture {
		val profileServiceWithMocks = new AbstractProfileService with MemberDaoComponent with StudentCourseDetailsDaoComponent with StaffAssistantsHelpers {
			val memberDao: MemberDao = mock[MemberDao]
			val studentCourseDetailsDao: StudentCourseDetailsDao = mock[StudentCourseDetailsDao]
			val staffAssistantsHelper: UserGroupMembershipHelperMethods[StaffMember] = mock[UserGroupMembershipHelperMethods[StaffMember]]
		}
	}

	@Test def disability() {
		new MockFixture {
			val disabilityQ = new Disability
			profileServiceWithMocks.memberDao.getDisability("Q") returns Some(disabilityQ)
			profileServiceWithMocks.memberDao.getDisability("SITS has no integrity checks") returns None

			profileServiceWithMocks.getDisability("Q") should be (Some(disabilityQ))
			profileServiceWithMocks.getDisability("SITS has no integrity checks") should be (None)
		}
	}

	@Transactional
	@Test def regenerateEmptyTimetableHash() {
		val member = Fixtures.student()
		member.timetableHash should be (null)

		profileService.save(member)
		session.flush()

		profileService.regenerateTimetableHash(member)
		session.clear()

		profileService.getMemberByUniversityId(member.universityId).get.timetableHash should not be null
	}

	@Transactional
	@Test def regenerateExistingTimetableHash() {
		val member = Fixtures.student()
		val existingHash = "1234567"
		member.timetableHash = existingHash
		member.timetableHash should be (existingHash)

		profileService.save(member)
		session.flush()

		profileService.regenerateTimetableHash(member)
		session.clear()

		profileService.getMemberByUniversityId(member.universityId).get.timetableHash should not be null
		profileService.getMemberByUniversityId(member.universityId).get.timetableHash should not be existingHash
	}

	@Test def memberByUser() { transactional { tx =>
		// TAB-2014
		val m1 = Fixtures.student(universityId = "1000001", userId="student")
		m1.email = "student@warwick.ac.uk"

		val m2 = Fixtures.staff(universityId = "1000002", userId="staff")
		m2.email = "staff@warwick.ac.uk"

		profileService.save(m1)
		profileService.save(m2)

		profileService.getMemberByUser(m1.asSsoUser) should be (Some(m1))
		profileService.getMemberByUser(m2.asSsoUser) should be (Some(m2))

		session.enableFilter(Member.StudentsOnlyFilter)

		profileService.getMemberByUser(m1.asSsoUser) should be (Some(m1))
		profileService.getMemberByUser(m2.asSsoUser) should be (None)
		profileService.getMemberByUser(m2.asSsoUser, disableFilter = true) should be (Some(m2))

		// Usercode matches, but warwickId doesn't. Still returns correctly
		profileService.getMemberByUser(m1.asSsoUser.tap(_.setWarwickId("0000001"))) should be (Some(m1))
		profileService.getMemberByUser(m2.asSsoUser.tap(_.setWarwickId("0000001")), disableFilter = true) should be (Some(m2))

		// Usercode doesn't match, but warwickId matches - but with the wrong email address
		profileService.getMemberByUser(new User("blabla").tap(_.setWarwickId("1000001"))) should be (None)
		profileService.getMemberByUser(new User("blabla").tap(_.setWarwickId("1000002")), disableFilter = true) should be (None)

		// Usercode doesn't match, but warwickId matches and so does email address
		profileService.getMemberByUser(new User("blabla").tap { u =>
			u.setWarwickId("1000001")
			u.setEmail("STUDENT@warwick.ac.uk   ")
		}) should be (Some(m1))
		profileService.getMemberByUser(new User("blabla").tap { u =>
			u.setWarwickId("1000002")
			u.setEmail("STAFF@warwick.ac.uk   ")
		}, disableFilter = true) should be (Some(m2))
	}}

}

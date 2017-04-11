package uk.ac.warwick.tabula.data.model.attendance

import org.joda.time.{DateTime, LocalDate}
import org.junit.Before
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.AutowiringAttendanceMonitoringDao
import uk.ac.warwick.tabula.data.model.{Department, Route, StudentMember, UserGroup}

class AttendanceMonitoringDaoTest extends PersistenceTestBase with Mockito {

	val academicYear = AcademicYear(2014)
	val department: Department = Fixtures.department("its")
	val route: Route = Fixtures.route("it100")

	val student1: StudentMember = Fixtures.student("1234","1234")
	student1.mostSignificantCourse.beginDate = DateTime.now.minusYears(2).toLocalDate
	student1.mostSignificantCourse.currentRoute = route
	student1.mostSignificantCourse.latestStudentCourseYearDetails.academicYear = academicYear

	val student2: StudentMember = Fixtures.student("2345","2345")
	student2.mostSignificantCourse.beginDate = DateTime.now.minusYears(2).toLocalDate
	student2.mostSignificantCourse.currentRoute = route
	student2.mostSignificantCourse.latestStudentCourseYearDetails.academicYear = academicYear

	val student3: StudentMember = Fixtures.student("2346","2346")
	student3.mostSignificantCourse.beginDate = DateTime.now.minusYears(2).toLocalDate
	student3.mostSignificantCourse.currentRoute = route
	student3.mostSignificantCourse.latestStudentCourseYearDetails.academicYear = academicYear

	val userLookup = new MockUserLookup
	userLookup.registerUserObjects(
		MemberOrUser(student1).asUser,
		MemberOrUser(student2).asUser
	)

	val attendanceMonitoringDao = new AutowiringAttendanceMonitoringDao {
		// Force the multi-query IN() clauses for 3 or more items
		override val maxInClause = 2
	}

	val scheme1 = new AttendanceMonitoringScheme
	scheme1.academicYear = academicYear
	scheme1.department = department
	scheme1.pointStyle = AttendanceMonitoringPointStyle.Date
	scheme1.members = UserGroup.ofUniversityIds
	scheme1.members.addUserId(student1.universityId)
	scheme1.members.addUserId(student2.universityId)
	scheme1.createdDate = DateTime.now
	scheme1.updatedDate = DateTime.now

	val point1: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, name = "point1")
	point1.startDate = new LocalDate(2014, 10, 1)
	point1.endDate = new LocalDate(2014, 10, 1)
	point1.pointType = AttendanceMonitoringPointType.Standard
	point1.createdDate = DateTime.now
	point1.updatedDate = DateTime.now

	val point2: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, name = "point2")
	point2.startDate = new LocalDate(2014, 10, 1)
	point2.endDate = new LocalDate(2014, 10, 4)
	point2.pointType = AttendanceMonitoringPointType.Standard
	point2.createdDate = DateTime.now
	point2.updatedDate = DateTime.now

	val point3: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, name = "point3")
	point3.startDate = new LocalDate(2014, 10, 4)
	point3.endDate = new LocalDate(2014, 10, 4)
	point3.pointType = AttendanceMonitoringPointType.Standard
	point3.createdDate = DateTime.now
	point3.updatedDate = DateTime.now

	val point4: AttendanceMonitoringPoint = Fixtures.attendanceMonitoringPoint(scheme1, name = "point4")
	point4.startDate = new LocalDate(2014, 10, 5)
	point4.endDate = new LocalDate(2014, 10, 5)
	point4.pointType = AttendanceMonitoringPointType.Standard
	point4.createdDate = DateTime.now
	point4.updatedDate = DateTime.now

	@Before
	def setup() {
		attendanceMonitoringDao.sessionFactory = sessionFactory
	}

	@Test def findRelevantPoints() { transactional { tx =>
		session.save(department)
		session.save(route)
		session.save(student1)
		session.save(student2)
		session.save(scheme1)
		session.save(point1)
		session.save(point2)
		session.save(point3)
		session.save(point4)

		val points = attendanceMonitoringDao.findRelevantPoints(department, academicYear, new LocalDate(2014, 10, 4))
		points.size should be (3)
		points.contains(point1) should be {true}
		points.contains(point2) should be {true}
		points.contains(point3) should be {true}
		// Point too late
		points.contains(point4) should be {false}
	}}

	@Test def allCheckpoints() { transactional { tx =>
		session.save(department)
		session.save(route)
		session.save(student1)
		session.save(student2)
		session.save(scheme1)
		session.save(point1)
		session.save(point2)
		session.save(point3)
		session.save(point4)

		val point1student1checkpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.Attended)
		point1student1checkpoint.updatedBy = ""
		point1student1checkpoint.updatedDate = DateTime.now
		session.save(point1student1checkpoint)

		for (s <- Seq(student1, student2)) {
			val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point2, s, AttendanceState.Attended)
			checkpoint.updatedBy = ""
			checkpoint.updatedDate = DateTime.now
			session.save(checkpoint)
		}

		val checkpoints = attendanceMonitoringDao.getAllCheckpoints(Seq(point1, point2, point3, point4))
		checkpoints.size should be (2)
		checkpoints(point1).size should be (1)
		checkpoints(point2).size should be (2)
		checkpoints(point3).size should be (0)
		checkpoints(point4).size should be (0)
	}}

	@Test def countCheckpointsForPoints() { transactional { tx =>
		session.save(department)
		session.save(route)
		session.save(student1)
		session.save(student2)
		session.save(scheme1)
		session.save(point1)
		session.save(point2)
		session.save(point3)
		session.save(point4)

		val point1student1checkpoint = Fixtures.attendanceMonitoringCheckpoint(point1, student1, AttendanceState.Attended)
		point1student1checkpoint.updatedBy = ""
		point1student1checkpoint.updatedDate = DateTime.now
		session.save(point1student1checkpoint)

		for (s <- Seq(student1, student2)) {
			val checkpoint = Fixtures.attendanceMonitoringCheckpoint(point2, s, AttendanceState.Attended)
			checkpoint.updatedBy = ""
			checkpoint.updatedDate = DateTime.now
			session.save(checkpoint)
		}

		val checkpoints = attendanceMonitoringDao.countCheckpointsForPoints(Seq(point1, point2, point3, point4))
		checkpoints.size should be (2)
		checkpoints(point1) should be (1)
		checkpoints(point2) should be (2)
		checkpoints(point3) should be (0)
		checkpoints(point4) should be (0)
	}}

	@Test def findNonReportedTerms() { transactional { tx =>
		session.save(department)
		session.save(route)
		session.save(student1)
		session.save(student2)
		session.save(student3)
		session.save(scheme1)
		session.save(point2)

		for (s <- Seq(student1, student2, student3)) {
			val report = new MonitoringPointReport()
			report.student = s
			report.academicYear = academicYear
			report.monitoringPeriod = "Spring"
			report.reporter = ""
			report.missed = 1
			report.createdDate = new DateTime()
			session.save(report)
		}

		val terms = attendanceMonitoringDao.findNonReportedTerms(Seq(student1, student2, student3), academicYear)

		// All students recorded against Spring, so that should be missing.
		terms should be (Seq("Autumn", "Christmas vacation", "Easter vacation", "Summer", "Summer vacation"))
	}}

	@Test def correctEndDate(): Unit = { transactional { tx =>
		// 2 SCDs, both null end date
		val newSCD = Fixtures.studentCourseDetails(student1, department, scjCode = "1234/2")
		newSCD.beginDate = DateTime.now.minusYears(2).toLocalDate
		newSCD.currentRoute = route
		newSCD.latestStudentCourseYearDetails.academicYear = academicYear
		session.save(department)
		session.save(route)
		session.save(student1)
		val result1 = attendanceMonitoringDao.getAttendanceMonitoringDataForStudents(Seq(student1.universityId), academicYear)
		result1.size should be (1)
		result1.head.scdEndDate.isEmpty should be {true}

		// 2 SCDs, 1 null end date
		newSCD.endDate = newSCD.beginDate.plusYears(3)
		session.update(newSCD)
		val result2 = attendanceMonitoringDao.getAttendanceMonitoringDataForStudents(Seq(student1.universityId), academicYear)
		result2.size should be (1)
		result2.head.scdEndDate.isEmpty should be {true}

		// 2 SCDs, no null end date
		student1.freshStudentCourseDetails.zipWithIndex.foreach{case(scd, i) =>
			scd.endDate = scd.beginDate.plusYears(i)
				session.update(scd)
		}
		val result3 = attendanceMonitoringDao.getAttendanceMonitoringDataForStudents(Seq(student1.universityId), academicYear)
		result3.size should be (1)
		result3.head.scdEndDate.nonEmpty should be {true}
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		result3.head.scdEndDate.get should be (student1.freshStudentCourseDetails.map(_.endDate).max)
	}}

}
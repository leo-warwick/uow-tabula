package uk.ac.warwick.tabula.data

import collection.JavaConverters._
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.junit.Before
import org.junit.After
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase, Fixtures}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.SitsStatus

// scalastyle:off magic.number
class MemberDaoTest extends PersistenceTestBase with Logging with Mockito {

	val memberDao = new MemberDaoImpl
	val sitsStatusDao = new SitsStatusDaoImpl
	val modeOfAttendanceDao = new ModeOfAttendanceDaoImpl
	
	val sprFullyEnrolledStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
	val sprPermanentlyWithdrawnStatus = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")
	
	val moaFT = Fixtures.modeOfAttendance("F", "FT", "Full time")
	val moaPT = Fixtures.modeOfAttendance("P", "PT", "Part time")

	@Before def setup() {
		memberDao.sessionFactory = sessionFactory
		sitsStatusDao.sessionFactory = sessionFactory
		modeOfAttendanceDao.sessionFactory = sessionFactory

		transactional { tx =>
			session.enableFilter(Member.ActiveOnlyFilter)
		}
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala map { session.delete(_) }
	}

	@Test
	def crud = {
		transactional { tx =>

			sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

			val m1 = Fixtures.student(universityId = "0000001", userId="student", sprStatus=sprFullyEnrolledStatus)
			val m2 = Fixtures.student(universityId = "0000002", userId="student", sprStatus=sprFullyEnrolledStatus)

			val m3 = Fixtures.staff(universityId = "0000003", userId="staff1")
			val m4 = Fixtures.staff(universityId = "0000004", userId="staff2")

			memberDao.saveOrUpdate(m1)
			memberDao.saveOrUpdate(m2)
			memberDao.saveOrUpdate(m3)
			memberDao.saveOrUpdate(m4)

			memberDao.getByUniversityId("0000001") should be (Some(m1))
			memberDao.getByUniversityId("0000002") should be (Some(m2))
			memberDao.getByUniversityId("0000003") should be (Some(m3))
			memberDao.getByUniversityId("0000004") should be (Some(m4))
			memberDao.getByUniversityId("0000005") should be (None)

			session.enableFilter(Member.StudentsOnlyFilter)

			memberDao.getByUniversityId("0000003") should be (None)
			memberDao.getByUniversityId("0000004") should be (None)

			memberDao.getAllByUserId("student", false) should be (Seq(m1, m2))
			memberDao.getByUserId("student", false) should be (Some(m1))
			memberDao.getAllByUserId("student", true) should be (Seq(m1, m2))
			memberDao.getAllByUserId("staff1", false) should be (Seq())
			memberDao.getByUserId("staff1", false) should be (None)
			memberDao.getAllByUserId("staff1", true) should be (Seq(m3))
			memberDao.getByUserId("staff1", true) should be (Some(m3))
			memberDao.getAllByUserId("unknown", false) should be (Seq())
			memberDao.getAllByUserId("unknown", true) should be (Seq())

			session.disableFilter(Member.StudentsOnlyFilter)

			memberDao.getAllByUserId("staff1", false) should be (Seq(m3))
			memberDao.getByUserId("staff1", false) should be (Some(m3))
	}
}

	@Test
  def listUpdatedSince = transactional { tx =>
		val dept1 = Fixtures.department("hi", "History")
		val dept2 = Fixtures.department("fr", "French")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(m1)
		memberDao.saveOrUpdate(m2)
		memberDao.saveOrUpdate(m3)
		memberDao.saveOrUpdate(m4)

		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be (Seq(m1, m2, m3, m4))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be (Seq(m1))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be (Seq(m2, m3, m4))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be (Seq())

		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 5) should be (Seq(m1, m2, m3))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 1) should be (Seq(m1))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), dept1, 5) should be (Seq(m2, m3))
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), dept1, 5) should be (Seq())
		memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept2, 5) should be (Seq(m4))
	}

	@Test
	def studentRelationshipsCurrentAndByTarget = transactional { tx =>
		val dept1 = Fixtures.department("sp", "Spanish")
		val dept2 = Fixtures.department("en", "English")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

	  val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		memberDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", relationshipType, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", relationshipType, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		memberDao.getCurrentRelationships(relationshipType, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getCurrentRelationships(relationshipType, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		memberDao.getCurrentRelationships(relationshipType, "1000003/1") should be (Nil)
		memberDao.getCurrentRelationships(null, "1000001/1") should be (Nil)

		memberDao.getRelationshipsByTarget(relationshipType, "1000001/1") should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getRelationshipsByTarget(relationshipType, "1000002/1") should be (Seq(relBetweenStaff1AndStu2))
		memberDao.getRelationshipsByTarget(relationshipType, "1000003/1") should be (Seq())
		memberDao.getRelationshipsByTarget(null, "1000001/1") should be (Seq())
	}

	@Test
	def studentRelationshipsByDepartmentAndAgent = transactional { tx =>
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		memberDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", relationshipType, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", relationshipType, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		// relationship Wires in a ProfileService, awkward.
		// Fortunately we have a chance to inject a mock in here.
		val profileService = smartMock[ProfileService]
		profileService.getStudentBySprCode("1000001/1") returns (Some(stu1))

		val ret = memberDao.getRelationshipsByDepartment(relationshipType, dept1)
		ret(0).profileService = profileService
		ret(0).studentMember.get.universityId should be ("1000001")
		ret(0).studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		memberDao.getRelationshipsByDepartment(relationshipType, dept1) should be (Seq(relBetweenStaff1AndStu1))
		memberDao.getRelationshipsByDepartment(relationshipType, dept2) should be (Seq(relBetweenStaff1AndStu2))

		memberDao.getRelationshipsByAgent(relationshipType, "1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		memberDao.getRelationshipsByAgent(relationshipType, "1000004") should be (Seq())

		memberDao.getAllRelationshipsByAgent("1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		memberDao.getAllRelationshipTypesByAgent("1000003") should be (Seq(relationshipType))
	}

	@Test
	def studentsWithoutRelationships = transactional { tx =>
		val dept1 = Fixtures.department("af", "Art of Foraging")
		val dept2 = Fixtures.department("tm", "Traditional Music")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		memberDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", relationshipType, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", relationshipType, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)

		memberDao.saveOrUpdate(m5)
		memberDao.saveOrUpdate(m6)

		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, dept1) should be (Seq(m5))
		memberDao.getStudentsWithoutRelationshipByDepartment(relationshipType, dept2) should be (Seq(m6))
		memberDao.getStudentsWithoutRelationshipByDepartment(null, dept1) should be (Seq())
	}

	@Test def studentRelationshipsByStaffDepartments = transactional{tx=>
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		memberDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu2 = StudentRelationship("1000004", relationshipType, "1000001/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		// relationship Wires in a ProfileService, awkward.
		// Fortunately we have a chance to inject a mock in here.
		val profileService = smartMock[ProfileService]
		profileService.getStudentBySprCode("1000001/1") returns (Some(stu1))

		val ret = memberDao.getRelationshipsByDepartment(relationshipType, dept1)
		ret(0).profileService = profileService
		ret(0).studentMember.get.universityId should be ("1000001")
		ret(0).studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		// staff department
		memberDao.getRelationshipsByStaffDepartment(relationshipType, dept2) should be (Seq(relBetweenStaff1AndStu2))

	}


	@Test def studentsCounting = transactional { tx =>
		val dept1 = Fixtures.department("ms", "Motorsport")
		val dept2 = Fixtures.department("vr", "Vehicle Repair")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		memberDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship("1000003", relationshipType, "1000001/1")
		val relBetweenStaff1AndStu2 = StudentRelationship("1000003", relationshipType, "1000002/1")

		memberDao.saveOrUpdate(relBetweenStaff1AndStu1)
		memberDao.saveOrUpdate(relBetweenStaff1AndStu2)

		memberDao.getStudentsByDepartment(dept1).size should be (1)
		memberDao.getStudentsByRelationshipAndDepartment(relationshipType, dept1).size should be (1)
	}
	
	@Test
	def getAllSprStatuses = transactional { tx =>
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)
		sitsStatusDao.saveOrUpdate(sprPermanentlyWithdrawnStatus)

		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprPermanentlyWithdrawnStatus)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(stu3)
		memberDao.saveOrUpdate(stu4)
		
		memberDao.getAllSprStatuses(dept1) should be (Seq(sprFullyEnrolledStatus))
		memberDao.getAllSprStatuses(dept2) should be (Seq(sprFullyEnrolledStatus, sprPermanentlyWithdrawnStatus))
	}
	
	@Test
	def getAllModesOfAttendance = transactional { tx =>
		modeOfAttendanceDao.saveOrUpdate(moaFT)
		modeOfAttendanceDao.saveOrUpdate(moaPT)

		val dept1 = Fixtures.department("hm", "History of Music")
		val dept2 = Fixtures.department("ar", "Architecture")

		session.save(dept1)
		session.save(dept2)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1)
		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2)
		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept2)
		val stu4 = Fixtures.student(universityId = "1000004", userId="student", department=dept2, courseDepartment=dept2)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(stu3)
		memberDao.saveOrUpdate(stu4)
		
		{
			val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance=moaFT)
			scyd.studentCourseDetails = stu1.mostSignificantCourse
			stu1.mostSignificantCourse.studentCourseYearDetails.add(scyd)
		}
		
		{
			val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance=moaFT)
			scyd.studentCourseDetails = stu2.mostSignificantCourse
			stu2.mostSignificantCourse.studentCourseYearDetails.add(scyd)
		}
		
		{
			val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance=moaFT)
			scyd.studentCourseDetails = stu3.mostSignificantCourse
			stu3.mostSignificantCourse.studentCourseYearDetails.add(scyd)
		}
		
		{
			val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance=moaPT)
			scyd.studentCourseDetails = stu4.mostSignificantCourse
			stu4.mostSignificantCourse.studentCourseYearDetails.add(scyd)
		}
		
		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(stu3)
		memberDao.saveOrUpdate(stu4)
		
		memberDao.getAllModesOfAttendance(dept1) should be (Seq(moaFT))
		memberDao.getAllModesOfAttendance(dept2) should be (Seq(moaFT, moaPT))
	}

}

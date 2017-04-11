package uk.ac.warwick.tabula.data

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{Fixtures, Mockito, PersistenceTestBase}

import scala.collection.JavaConverters.asScalaBufferConverter

// scalastyle:off magic.number
class RelationshipDaoTest extends PersistenceTestBase with Logging with Mockito {

	val memberDao = new AutowiringMemberDaoImpl
	val relationshipDao = new RelationshipDaoImpl
	val sitsStatusDao = new SitsStatusDaoImpl

	val sprFullyEnrolledStatus: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
	val sprPermanentlyWithdrawnStatus: SitsStatus = Fixtures.sitsStatus("P", "Permanently Withdrawn", "Permanently Withdrawn")

	@Before def setup() {
		relationshipDao.sessionFactory = sessionFactory
		memberDao.sessionFactory = sessionFactory
		sitsStatusDao.sessionFactory = sessionFactory

		transactional { tx =>
			session.enableFilter(Member.ActiveOnlyFilter)
		}
	}

	@After def tidyUp(): Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala foreach { session.delete(_) }
	}

	@Test
	def studentRelationshipsCurrentAndByTarget() = transactional { tx =>
		val dept1 = Fixtures.department("sp", "Spanish")
		val dept2 = Fixtures.department("en", "English")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val stu3 = Fixtures.student(universityId = "1000003", userId="student", department=dept2, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)
		memberDao.saveOrUpdate(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship(staff1, relationshipType, stu1, DateTime.now)
		val relBetweenStaff1AndStu2 = StudentRelationship(staff1, relationshipType, stu2, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu1)
		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)

		relationshipDao.getCurrentRelationships(relationshipType, stu1) should be (Seq(relBetweenStaff1AndStu1))
		relationshipDao.getCurrentRelationships(relationshipType, stu2) should be (Seq(relBetweenStaff1AndStu2))
		relationshipDao.getCurrentRelationships(relationshipType, stu3) should be (Nil)
		relationshipDao.getCurrentRelationships(null, stu1) should be (Nil)

		relationshipDao.getRelationshipsByTarget(relationshipType, stu1) should be (Seq(relBetweenStaff1AndStu1))
		relationshipDao.getRelationshipsByTarget(relationshipType, stu2) should be (Seq(relBetweenStaff1AndStu2))
		relationshipDao.getRelationshipsByTarget(relationshipType, stu3) should be (Seq())
		relationshipDao.getRelationshipsByTarget(null, stu1) should be (Seq())

		relationshipDao.getRelationshipsByCourseDetails(relationshipType, stu1.mostSignificantCourse) should be (Seq(relBetweenStaff1AndStu1))
		relationshipDao.getRelationshipsByCourseDetails(relationshipType, stu3.mostSignificantCourse) should be (Seq())

		relationshipDao.getCurrentRelationship(relationshipType, stu1, staff1) should be (Some(relBetweenStaff1AndStu1))
	}

	@Test
	def studentRelationshipsByDepartmentAndAgent() = transactional { tx =>
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
		relationshipDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship(staff1, relationshipType, stu1, DateTime.now)
		val relBetweenStaff1AndStu2 = StudentRelationship(staff1, relationshipType, stu2, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu1)
		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)

		val ret = relationshipDao.getCurrentRelationshipsByDepartment(relationshipType, dept1)
		ret.head.studentMember.get.universityId should be ("1000001")
		ret.head.studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		relationshipDao.getCurrentRelationshipsByDepartment(relationshipType, dept1) should be (Seq(relBetweenStaff1AndStu1))
		relationshipDao.getCurrentRelationshipsByDepartment(relationshipType, dept2) should be (Seq(relBetweenStaff1AndStu2))

		relationshipDao.getCurrentRelationshipsByAgent(relationshipType, "1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		relationshipDao.getCurrentRelationshipsByAgent(relationshipType, "1000004") should be (Seq())

		relationshipDao.getCurrentRelationshipsForAgent("1000003").toSet should be (Seq(relBetweenStaff1AndStu1, relBetweenStaff1AndStu2).toSet)
		relationshipDao.getCurrentRelationshipTypesByAgent("1000003") should be (Seq(relationshipType))
	}

	@Test
	def studentsWithoutRelationships() = transactional { tx =>
		val dept1 = Fixtures.department("af", "Art of Foraging")
		val dept2 = Fixtures.department("tm", "Traditional Music")

		session.save(dept1)
		session.save(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipDao.saveOrUpdate(relationshipType)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val stu2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		memberDao.saveOrUpdate(stu1)
		memberDao.saveOrUpdate(stu2)
		memberDao.saveOrUpdate(staff1)

		val relBetweenStaff1AndStu1 = StudentRelationship(staff1, relationshipType, stu1, DateTime.now)
		val relBetweenStaff1AndStu2 = StudentRelationship(staff1, relationshipType, stu2, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu1)
		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)

		memberDao.saveOrUpdate(m5)
		memberDao.saveOrUpdate(m6)

		relationshipDao.getStudentsWithoutCurrentRelationshipByDepartment(relationshipType, dept1) should be (Seq(m5))
		relationshipDao.getStudentsWithoutCurrentRelationshipByDepartment(relationshipType, dept2) should be (Seq(m6))
		relationshipDao.getStudentsWithoutCurrentRelationshipByDepartment(null, dept1) should be (Seq())
	}

	@Test def studentRelationshipsByStaffDepartments() = transactional{tx=>
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
		relationshipDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu2 = StudentRelationship(staff2, relationshipType, stu1, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)

		val ret = relationshipDao.getCurrentRelationshipsByDepartment(relationshipType, dept1)
		ret.head.studentMember.get.universityId should be ("1000001")
		ret.head.studentMember.get.mostSignificantCourseDetails.get.department.code should be ("hm")

		// staff department
		relationshipDao.getCurrentRelationshipsByStaffDepartment(relationshipType, dept2) should be (Seq(relBetweenStaff1AndStu2))

	}

	@Test def studentsByAgentRelationship() = transactional { tx =>
		val dept1 = Fixtures.department("ml", "Modern Languages")
		val dept2 = Fixtures.department("fr", "French")
		val dept3 = Fixtures.department("es", "Spanish")

		// add 2 sub-departments and ensure no dupes TAB-1811
		dept2.parent = dept1
		dept3.parent = dept1
		dept1.children.add(dept2)
		dept1.children.add(dept3)

		session.save(dept1)
		session.save(dept2)
		session.save(dept3)

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
		relationshipDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndStu1 = StudentRelationship(staff1, relationshipType, stu1, DateTime.now)
		val relBetweenStaff1AndStu2 = StudentRelationship(staff1, relationshipType, stu2, DateTime.now)

		val relBetweenStaff2AndStu1 = StudentRelationship(staff2, relationshipType, stu1, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu1)
		relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)
		relationshipDao.saveOrUpdate(relBetweenStaff2AndStu1)

		memberDao.getStudentsByDepartment(dept1).size should be (1)
		memberDao.getStudentsByDepartment(dept2).size should be (1)
		relationshipDao.getStudentsByRelationshipAndDepartment(relationshipType, dept1).size should be (1)

		memberDao.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staff1.universityId, Seq()).size should be (2)
		memberDao.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staff2.universityId, Seq()).size should be (1)
	}

	@Test def studentsByAgentRelationshipMultiScds() = transactional { tx =>

		val dept = Fixtures.department("ml", "Modern Languages")
		session.save(dept)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val studentWithMultipleScdsSameSpr = Fixtures.student(universityId = "1000001", userId="student", department=dept, courseDepartment=dept, sprStatus=sprFullyEnrolledStatus)
		studentWithMultipleScdsSameSpr.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val sprCode = "0123456/2"

		// Student with multiple StudentCourseDetails with same SPR code - ensure no dupes TAB-1825
		val scd = new StudentCourseDetails(studentWithMultipleScdsSameSpr, "0123456/1")
		scd.sprCode = sprCode
		val scd2 = new StudentCourseDetails(studentWithMultipleScdsSameSpr, "0123456/3")
		scd2.sprCode = sprCode

		studentWithMultipleScdsSameSpr.attachStudentCourseDetails(scd)
		studentWithMultipleScdsSameSpr.attachStudentCourseDetails(scd2)

		val anotherStudent = Fixtures.student(universityId = "1000002", userId="student", department=dept, courseDepartment=dept, sprStatus=sprFullyEnrolledStatus)
		anotherStudent.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val staff = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept)
		staff.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		memberDao.saveOrUpdate(studentWithMultipleScdsSameSpr)
		memberDao.saveOrUpdate(anotherStudent)
		memberDao.saveOrUpdate(staff)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipDao.saveOrUpdate(relationshipType)

		val relBetweenStaff1AndMultiSCDStudent = StudentRelationship(staff, relationshipType, studentWithMultipleScdsSameSpr, DateTime.now)
		val relBetweenStaff1AndOtherStudent = StudentRelationship(staff, relationshipType, anotherStudent, DateTime.now)

		relationshipDao.saveOrUpdate(relBetweenStaff1AndMultiSCDStudent)
		relationshipDao.saveOrUpdate(relBetweenStaff1AndOtherStudent)

		memberDao.getStudentsByDepartment(dept).size should be (2)

		memberDao.getSCDsByAgentRelationshipAndRestrictions(relationshipType, staff.universityId, Seq()).size should be (2)
	}

}

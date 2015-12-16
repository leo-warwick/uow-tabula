package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters.asScalaBufferConverter
import org.joda.time.{DateTime, DateTimeConstants, DateTimeUtils}
import org.junit.{After, Before}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.{AppContextTestBase, Fixtures}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.SitsStatusDaoImpl
import uk.ac.warwick.tabula.data.model.{StudentRelationship, Member, StudentRelationshipType}

// scalastyle:off magic.number
class RelationshipServiceTest extends AppContextTestBase with Mockito {

	@Autowired var relationshipService: RelationshipServiceImpl = _
	@Autowired var profileService: ProfileServiceImpl = _
	val sitsStatusDao = new SitsStatusDaoImpl
	val sprFullyEnrolledStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
	var sprWithdrawnStatus = Fixtures.sitsStatus("PX", "PWD X", "Permanently Withdrawn with Nuance")

	trait Environment {
		val student = Fixtures.student(universityId="0102030")
		val studentCourseDetails = student.mostSignificantCourseDetails.get

		val staff1 = Fixtures.staff(universityId = "1234567")
		val staff2 = Fixtures.staff(universityId = "7654321")

		session.save(student)
		session.save(staff1)
		session.save(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)
	}

	@Transactional
	@Test def findingRelationships = withFakeTime(dateTime(2000, 6)) {
		new Environment {
			relationshipService.findCurrentRelationships(relationshipType, student) should be('empty)
			relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(staff1))

			val opt = relationshipService.findCurrentRelationships(relationshipType, student).headOption
			val currentRelationship = opt.getOrElse(fail("Failed to get current relationship"))
			currentRelationship.agent should be("1234567")

			// now we've stored a relationship.  Try storing the identical one again:
			relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(staff1))

			relationshipService.findCurrentRelationships(relationshipType, student).headOption.getOrElse(fail("Failed to get current relationship after re-storing")).agent should be("1234567")
			relationshipService.getRelationships(relationshipType, student).size should be(1)

			// now store a new personal tutor for the same student. We should now have TWO personal tutors (as of TAB-416)
			DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
			relationshipService.saveStudentRelationships(relationshipType, studentCourseDetails, Seq(staff2))

			val rels = relationshipService.getRelationships(relationshipType, student)

			DateTimeUtils.setCurrentMillisFixed(new DateTime().plusMillis(30).getMillis())
			val currentRelationshipsUpdated = relationshipService.findCurrentRelationships(relationshipType, student)
			currentRelationshipsUpdated.size should be(2)

			currentRelationshipsUpdated.find(_.agent == "7654321") should be('defined)

			relationshipService.getRelationships(relationshipType, student).size should be(2)

			val stu = Fixtures.student(universityId = "1250148", userId = "student", sprStatus = sprFullyEnrolledStatus)
			stu.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

			profileService.save(stu)

			relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1234567").size should be(1)
			relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "7654321").size should be(1)
			relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "7654321").head.studentMember should be(Some(student))
		}
	}

	@Before def setup: Unit = transactional { tx =>
		sitsStatusDao.sessionFactory = sessionFactory
		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		session.enableFilter(Member.ActiveOnlyFilter)
	}

	@After def tidyUp: Unit = transactional { tx =>
		session.disableFilter(Member.ActiveOnlyFilter)

		session.newCriteria[Member].seq.foreach(session.delete)
	}

	@Test def saveFindGetStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("el", "Equatorial Life")
		val dept2 = Fixtures.department("nr", "Northern Regions")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, m1.mostSignificantCourseDetails.get, Seq(m3)).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, m2.mostSignificantCourseDetails.get, Seq(m3)).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.findCurrentRelationships(relationshipType, m1) should be (Seq(rel1))
		relationshipService.findCurrentRelationships(relationshipType, m2) should be (Seq(rel2))
		relationshipService.findCurrentRelationships(relationshipType, Fixtures.student(universityId = "1000003")) should be (Nil)
		relationshipService.findCurrentRelationships(null, m1) should be (Nil)

		relationshipService.getRelationships(relationshipType, m1) should be (Seq(rel1))
		relationshipService.getRelationships(relationshipType, m2) should be (Seq(rel2))
		relationshipService.getRelationships(relationshipType, Fixtures.student(universityId = "1000003")) should be (Seq())
		relationshipService.getRelationships(null, m1) should be (Seq())
	}

	@Test def saveReplaceStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("el", "Equatorial Life")
		val dept2 = Fixtures.department("nr", "Northern Regions")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)
		session.flush()

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val stu1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, sprStatus=sprFullyEnrolledStatus)
		stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val staff1 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val staff2 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(stu1)
		profileService.save(staff1)
		profileService.save(staff2)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		// create and save one personal tutor relationship
		val relStu1Staff3 = relationshipService.saveStudentRelationships(relationshipType, stu1.mostSignificantCourseDetails.get, Seq(staff1)).head
		session.save(relStu1Staff3)
		relationshipService.getRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3))
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3))

		// now try saving the same again - shouldn't make any difference
		val relStu1Staff3_2ndAttempt = relationshipService.saveStudentRelationships(relationshipType, stu1.mostSignificantCourseDetails.get, Seq(staff1)).head
		session.save(relStu1Staff3_2ndAttempt)
		relationshipService.getRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3))
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3))

		// now replace the existing personal tutor with a new one
		relStu1Staff3.endDate should be (null)
		val relStu1Staff4 = relationshipService.replaceStudentRelationships(relationshipType, stu1.mostSignificantCourseDetails.get, Seq(staff2)).head
		session.save(relStu1Staff4)
		relationshipService.getRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3, relStu1Staff4))
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq(relStu1Staff4))
		relStu1Staff3.endDate should not be (null)

		// now save the original personal tutor again:
		val relStu1Staff3_3rdAttempt = relationshipService.saveStudentRelationships(relationshipType, stu1.mostSignificantCourseDetails.get, Seq(staff1)).head
		session.save(relStu1Staff3_3rdAttempt)
		relationshipService.getRelationships(relationshipType, stu1) should be (Seq(relStu1Staff3, relStu1Staff4, relStu1Staff3_3rdAttempt))
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq(relStu1Staff4, relStu1Staff3_3rdAttempt))
		relStu1Staff4.endDate should be (null)
		relStu1Staff3_3rdAttempt.endDate should be (null)

		// If there is an old relationship but no current relationship, the relationship should be recreated.
		// End the current relationships:
		relStu1Staff3_3rdAttempt.endDate = DateTime.now.minusMinutes(5)
		relStu1Staff4.endDate = DateTime.now.minusMinutes(5)
		session.saveOrUpdate(relStu1Staff3_3rdAttempt)
		session.saveOrUpdate(relStu1Staff4)
		session.flush
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq())

		// then recreate one of them using a call to replaceStudentRelationship:
		val relStu1Staff4_2ndAttempt = relationshipService.replaceStudentRelationships(relationshipType, stu1.mostSignificantCourseDetails.get, Seq(staff2)).head
		session.save(relStu1Staff4_2ndAttempt)
		session.flush
		relationshipService.findCurrentRelationships(relationshipType, stu1) should be (Seq(relStu1Staff4_2ndAttempt))
	}


	@Test def listStudentRelationships = transactional { tx =>

		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, m1.mostSignificantCourseDetails.get, Seq(m3)).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, m2.mostSignificantCourseDetails.get, Seq(m3)).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.listStudentRelationshipsByDepartment(relationshipType, dept1) should be (Seq(rel1))
		relationshipService.listStudentRelationshipsByDepartment(relationshipType, dept2) should be (Seq(rel2))

		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1000003").toSet should be (Seq(rel1, rel2).toSet)
		relationshipService.listStudentRelationshipsWithUniversityId(relationshipType, "1000004") should be (Seq())
	}

	@Test def listStudentsWithoutRelationship = transactional { tx =>

		val dept1 = Fixtures.department("mm", "Mumbling Modularity")
		val dept2 = Fixtures.department("dd", "Departmental Diagnostics")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

		val m5 = Fixtures.student(universityId = "1000005", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		val m6 = Fixtures.student(universityId = "1000006", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprFullyEnrolledStatus)

		session.saveOrUpdate(m5)
		session.saveOrUpdate(m6)

		val ugCourse = Fixtures.course("UBLAH", "Some UG course")
		session.save(ugCourse)

		val route1 = Fixtures.route("UXXX", "Some route")
		route1.adminDepartment = dept1
		session.save(route1)

		val route2 = Fixtures.route("UYYY", "Some other route")
		route2.adminDepartment = dept2
		session.save(route2)

		session.flush()
		session.clear()

		val scd5 = m5.mostSignificantCourse
		scd5.course = ugCourse
		scd5.route = route1
		session.saveOrUpdate(scd5)
		session.flush()
		session.clear()

		val scd6 = m6.mostSignificantCourse
		scd6.course = ugCourse
		scd6.route = route2
		session.saveOrUpdate(scd6)
		session.flush()
		session.clear()

		session.saveOrUpdate(m5)
		session.saveOrUpdate(m6)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipType.expectedUG = true
		relationshipService.saveOrUpdate(relationshipType)

		relationshipService.listStudentsWithoutRelationship(relationshipType, dept1) should be (Seq(m5))
		relationshipService.listStudentsWithoutRelationship(relationshipType, dept2) should be (Seq(m6))
		relationshipService.listStudentsWithoutRelationship(null, dept1) should be (Seq())
	}

	@Test def testRelationshipNotPermanentlyWithdrawn = transactional { tx =>
		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")
		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)
		sitsStatusDao.saveOrUpdate(sprWithdrawnStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprWithdrawnStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

		val m3 = Fixtures.staff(universityId = "1000003", userId="staff1", department=dept1)
		m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

		val m4 = Fixtures.staff(universityId = "1000004", userId="staff2", department=dept2)
		m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

		profileService.save(m1)
		profileService.save(m2)
		profileService.save(m3)
		profileService.save(m4)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		relationshipService.saveOrUpdate(relationshipType)

		val rel1 = relationshipService.saveStudentRelationships(relationshipType, m1.mostSignificantCourseDetails.get, Seq(m3)).head
		val rel2 = relationshipService.saveStudentRelationships(relationshipType, m2.mostSignificantCourseDetails.get, Seq(m3)).head

		session.save(rel1)
		session.save(rel2)

		relationshipService.relationshipNotPermanentlyWithdrawn(rel1) should be (true)
		relationshipService.relationshipNotPermanentlyWithdrawn(rel2) should be (false)
	}

	@Test def testExpectedToHaveRelationship = transactional { tx =>
		val dept1 = Fixtures.department("pe", "Polar Exploration")
		val dept2 = Fixtures.department("mi", "Micrology")

		session.saveOrUpdate(dept1)
		session.saveOrUpdate(dept2)

		val ugCourse = Fixtures.course("UBLAH", "Some UG course")
		val pgtCourse = Fixtures.course("TBLAH", "Some PGT course")
		val pgrCourse = Fixtures.course("RBLAH", "Some PGR course")

		session.saveOrUpdate(ugCourse)
		session.saveOrUpdate(pgtCourse)
		session.saveOrUpdate(pgrCourse)

		val route1 = Fixtures.route("UXXX", "Some route")
		route1.adminDepartment = dept1
		session.save(route1)

		val route2 = Fixtures.route("UYYY", "Some other route")
		route2.adminDepartment = dept2
		session.save(route2)

		sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)
		sitsStatusDao.saveOrUpdate(sprWithdrawnStatus)

		val m1 = Fixtures.student(universityId = "1000001", userId="student", department=dept1, courseDepartment=dept1, sprStatus=sprFullyEnrolledStatus)
		m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

		val scd1 = m1.mostSignificantCourse
		scd1.course = ugCourse
		scd1.route = route1

		profileService.save(m1)

		val ptRelType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		ptRelType.expectedUG = true
		relationshipService.saveOrUpdate(ptRelType)

		relationshipService.studentDepartmentMatchesAndExpectedToHaveRelationship(ptRelType, dept1)(m1) should be (true)

		val m2 = Fixtures.student(universityId = "1000002", userId="student", department=dept2, courseDepartment=dept2, sprStatus=sprWithdrawnStatus)
		m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)
		profileService.save(m2)

		val scd2 = m2.mostSignificantCourse
		scd2.course = pgtCourse
		scd2.route = route2

		session.saveOrUpdate(scd2)

		relationshipService.studentDepartmentMatchesAndExpectedToHaveRelationship(ptRelType, dept2)(m2) should be (false)

		// TAB-1712
		scd2.route = null
		session.saveOrUpdate(scd2)
		relationshipService.studentDepartmentMatchesAndExpectedToHaveRelationship(ptRelType, dept2)(m2) should be (false)
	}

	@Transactional
	@Test def testEndStudentRelationship {
		new Environment {
			val newRelationship = StudentRelationship(staff1, relationshipType, studentCourseDetails)
			newRelationship.endDate should be (null)
			session.saveOrUpdate(newRelationship)
			session.flush()
			session.clear()
			val relationshipService = Wire[RelationshipService]
			val relsFromDb = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
			relsFromDb.size should be (1)
			relsFromDb.head.endDate should be (null)
			relationshipService.endStudentRelationships(relsFromDb)
			session.flush()
			session.clear()
			val relsFromDb2 = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)
			relsFromDb.size should be (1)
			val endDate =relsFromDb.head.endDate
			endDate should not be (null)
			endDate.isBefore(new DateTime())
		}
	}
}

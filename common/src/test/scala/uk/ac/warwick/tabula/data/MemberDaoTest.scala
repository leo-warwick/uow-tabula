package uk.ac.warwick.tabula.data

import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.{Fixtures, Mockito, PersistenceTestBase}

import scala.collection.JavaConverters.asScalaBufferConverter

// scalastyle:off magic.number
class MemberDaoTest extends PersistenceTestBase with Logging with Mockito {

  val memberDao = new AutowiringMemberDaoImpl
  val relationshipDao = new RelationshipDaoImpl
  val sitsStatusDao = new SitsStatusDaoImpl
  val modeOfAttendanceDao = new ModeOfAttendanceDaoImpl

  val sprFullyEnrolledStatus: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
  val sprPermanentlyWithdrawnStatus: SitsStatus = Fixtures.sitsStatus("P", "PWD", "Permanently Withdrawn")

  val moaFT: ModeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
  val moaPT: ModeOfAttendance = Fixtures.modeOfAttendance("P", "PT", "Part time")

  @Before def setup(): Unit = {
    memberDao.sessionFactory = sessionFactory
    relationshipDao.sessionFactory = sessionFactory
    sitsStatusDao.sessionFactory = sessionFactory
    modeOfAttendanceDao.sessionFactory = sessionFactory

    transactional { tx =>
      session.enableFilter(Member.ActiveOnlyFilter)
    }
  }

  @After def tidyUp(): Unit = transactional { tx =>
    session.disableFilter(Member.ActiveOnlyFilter)

    session.createCriteria(classOf[Member]).list().asInstanceOf[JList[Member]].asScala foreach {
      session.delete
    }
  }

  @Test
  def crud(): Unit = {
    transactional { tx =>

      sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

      val m1 = Fixtures.student(universityId = "0000001", userId = "student", sprStatus = sprFullyEnrolledStatus)
      val m2 = Fixtures.student(universityId = "0000002", userId = "student", sprStatus = sprFullyEnrolledStatus)

      val m3 = Fixtures.staff(universityId = "0000003", userId = "staff1")
      val m4 = Fixtures.staff(universityId = "0000004", userId = "staff2")

      memberDao.saveOrUpdate(m1)
      memberDao.saveOrUpdate(m2)
      memberDao.saveOrUpdate(m3)
      memberDao.saveOrUpdate(m4)

      memberDao.getByUniversityId("0000001") should be(Some(m1))
      memberDao.getByUniversityId("0000002") should be(Some(m2))
      memberDao.getByUniversityId("0000003") should be(Some(m3))
      memberDao.getByUniversityId("0000004") should be(Some(m4))
      memberDao.getByUniversityId("0000005") should be(None)

      session.enableFilter(Member.StudentsOnlyFilter)

      memberDao.getByUniversityId("0000003") should be(None)
      memberDao.getByUniversityId("0000004") should be(None)

      memberDao.getAllByUserId("student", disableFilter = false) should be(Seq(m1, m2))
      memberDao.getAllByUserId("student", disableFilter = true) should be(Seq(m1, m2))
      memberDao.getAllByUserId("staff1", disableFilter = false) should be(Seq())
      memberDao.getAllByUserId("staff1", disableFilter = true) should be(Seq(m3))
      memberDao.getAllByUserId("unknown", disableFilter = false) should be(Seq())
      memberDao.getAllByUserId("unknown", disableFilter = true) should be(Seq())

      session.disableFilter(Member.StudentsOnlyFilter)

      memberDao.getAllByUserId("staff1", disableFilter = false) should be(Seq(m3))
    }
  }

  @Transactional
  @Test
  def testGetStudentByTimetableHash(): Unit = {
    val student = Fixtures.student()
    val timetableHash = "abc"
    student.timetableHash = timetableHash
    memberDao.saveOrUpdate(student)
    memberDao.getMemberByTimetableHash(timetableHash) should be(Some(student))
    memberDao.getMemberByTimetableHash("anotherhash") should be(None)
  }

  @Test
  def listUpdatedSince(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hi", "History")
    val dept2 = Fixtures.department("fr", "French")

    session.save(dept1)
    session.save(dept2)

    sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

    val m1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, sprStatus = sprFullyEnrolledStatus)
    m1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

    val m2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept1, sprStatus = sprFullyEnrolledStatus)
    m2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

    val m3 = Fixtures.staff(universityId = "1000003", userId = "staff1", department = dept1)
    m3.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

    val m4 = Fixtures.staff(universityId = "1000004", userId = "staff2", department = dept2)
    m4.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

    memberDao.saveOrUpdate(m1)
    memberDao.saveOrUpdate(m2)
    memberDao.saveOrUpdate(m3)
    memberDao.saveOrUpdate(m4)

    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 5) should be(Seq(m1, m2, m3, m4))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), 1) should be(Seq(m1))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), 5) should be(Seq(m2, m3, m4))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), 5) should be(Seq())

    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 5) should be(Seq(m1, m2, m3))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept1, 1) should be(Seq(m1))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 0, 0, 0, 0), dept1, 5) should be(Seq(m2, m3))
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.FEBRUARY, 5, 0, 0, 0, 0), dept1, 5) should be(Seq())
    memberDao.listUpdatedSince(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0), dept2, 5) should be(Seq(m4))
  }

  @Test def studentsCounting(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("ms", "Motorsport")
    val dept2 = Fixtures.department("vr", "Vehicle Repair")

    session.save(dept1)
    session.save(dept2)

    sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1, sprStatus = sprFullyEnrolledStatus)
    stu1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 1, 0, 0, 0)

    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2, sprStatus = sprFullyEnrolledStatus)
    stu2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 1, 0, 0, 0)

    val staff1 = Fixtures.staff(universityId = "1000003", userId = "staff1", department = dept1)
    staff1.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 1, 0, 0, 0)

    val staff2 = Fixtures.staff(universityId = "1000004", userId = "staff2", department = dept2)
    staff2.lastUpdatedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 1, 0, 0, 0)

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(staff1)
    memberDao.saveOrUpdate(staff2)

    val relationshipType = relationshipDao.getStudentRelationshipTypeById("personalTutor").get

    val relBetweenStaff1AndStu1 = StudentRelationship(staff1, relationshipType, stu1, DateTime.now)
    val relBetweenStaff1AndStu2 = StudentRelationship(staff1, relationshipType, stu2, DateTime.now)

    relationshipDao.saveOrUpdate(relBetweenStaff1AndStu1)
    relationshipDao.saveOrUpdate(relBetweenStaff1AndStu2)

    memberDao.getStudentsByDepartment(dept1).size should be(1)
    relationshipDao.getStudentsByRelationshipAndDepartment(relationshipType, dept1).size should be(1)
  }

  @Test
  def testGetAllSprStatuses(): Unit = transactional { tx =>
    sitsStatusDao.saveOrUpdate(sprFullyEnrolledStatus)
    sitsStatusDao.saveOrUpdate(sprPermanentlyWithdrawnStatus)

    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.save(dept1)
    session.save(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1, sprStatus = sprFullyEnrolledStatus)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2, sprStatus = sprFullyEnrolledStatus)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student", department = dept2, courseDepartment = dept2, sprStatus = sprFullyEnrolledStatus)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student", department = dept2, courseDepartment = dept2, sprStatus = sprPermanentlyWithdrawnStatus)

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.getAllSprStatuses(dept1) should be(Seq(sprFullyEnrolledStatus))
    memberDao.getAllSprStatuses(dept2) should be(Seq(sprFullyEnrolledStatus, sprPermanentlyWithdrawnStatus))
  }

  @Test
  def testGetAllModesOfAttendance(): Unit = transactional { tx =>
    modeOfAttendanceDao.saveOrUpdate(moaFT)
    modeOfAttendanceDao.saveOrUpdate(moaPT)

    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.save(dept1)
    session.save(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student", department = dept2, courseDepartment = dept2)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student", department = dept2, courseDepartment = dept2)

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

  {
    val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance = moaFT)
    scyd.studentCourseDetails = stu1.mostSignificantCourse
    scyd.sceSequenceNumber = 2
    stu1.mostSignificantCourse.addStudentCourseYearDetails(scyd)
    stu1.mostSignificantCourse.latestStudentCourseYearDetails = scyd
  }

  {
    val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance = moaFT)
    scyd.studentCourseDetails = stu2.mostSignificantCourse
    scyd.sceSequenceNumber = 2
    stu2.mostSignificantCourse.addStudentCourseYearDetails(scyd)
    stu2.mostSignificantCourse.latestStudentCourseYearDetails = scyd
  }

  {
    val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance = moaFT)
    scyd.studentCourseDetails = stu3.mostSignificantCourse
    scyd.sceSequenceNumber = 2
    stu3.mostSignificantCourse.addStudentCourseYearDetails(scyd)
    stu3.mostSignificantCourse.latestStudentCourseYearDetails = scyd
  }

  {
    val scyd = Fixtures.studentCourseYearDetails(modeOfAttendance = moaPT)
    scyd.studentCourseDetails = stu4.mostSignificantCourse
    scyd.sceSequenceNumber = 2
    stu4.mostSignificantCourse.addStudentCourseYearDetails(scyd)
    stu4.mostSignificantCourse.latestStudentCourseYearDetails = scyd
  }

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.getAllModesOfAttendance(dept1) should be(Seq(moaFT))
    memberDao.getAllModesOfAttendance(dept2) should be(Seq(moaFT, moaPT))
  }

  @Test
  def testGetFreshAndStaleUniversityIds(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.saveOrUpdate(dept1)
    session.saveOrUpdate(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student", department = dept2, courseDepartment = dept2)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student", department = dept2, courseDepartment = dept2)

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.getFreshStudentUniversityIds.size should be(4)
    memberDao.getAllWithUniversityIdsStaleOrFresh(Seq("1000001", "1000002", "1000003", "1000004")).size should be(4)

    stu3.missingFromImportSince = DateTime.now
    memberDao.saveOrUpdate(stu3)
    session.flush()

    memberDao.getFreshStudentUniversityIds.size should be(3)
    memberDao.getAllWithUniversityIdsStaleOrFresh(Seq("1000001", "1000002", "1000003", "1000004")).size should be(4)

    memberDao.getByUniversityId("1000003") should be(None)
    memberDao.getByUniversityIdStaleOrFresh("1000003").get.universityId should be("1000003")
  }

  @Test
  def testStampMissingFromImport(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.saveOrUpdate(dept1)
    session.saveOrUpdate(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student", department = dept2, courseDepartment = dept2)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student", department = dept2, courseDepartment = dept2)

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.getByUniversityId("1000001").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000002").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000003").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000004").get.missingFromImportSince should be(null)

    val newStaleIds = Seq[String]("1000002")

    val importStart = DateTime.now
    memberDao.stampMissingFromImport(newStaleIds, importStart)
    session.flush()
    session.clear()

    memberDao.getByUniversityId("1000001").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000003").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000004").get.missingFromImportSince should be(null)

    memberDao.getByUniversityId("1000002") should be(None)

  }

  @Test
  def testUnstampPresentInImport(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.saveOrUpdate(dept1)
    session.saveOrUpdate(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student", department = dept2, courseDepartment = dept2)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student", department = dept2, courseDepartment = dept2)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student", department = dept2, courseDepartment = dept2)

    stu2.missingFromImportSince = DateTime.now()
    stu3.missingFromImportSince = DateTime.now()
    stu4.missingFromImportSince = DateTime.now()

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.getByUniversityId("1000001").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000002") should be(None)
    memberDao.getByUniversityId("1000003") should be(None)
    memberDao.getByUniversityId("1000004") should be(None)

    memberDao.unstampPresentInImport(Seq("1000002"))
    session.flush()
    session.clear()

    memberDao.getByUniversityId("1000001").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000002").get.missingFromImportSince should be(null)
    memberDao.getByUniversityId("1000003") should be(None)
    memberDao.getByUniversityId("1000004") should be(None)

  }

  @Test
  def testFindUndergraduateStudentsByHomeDepartmentAndLevel(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hm", "History of Music")
    val dept2 = Fixtures.department("ar", "Architecture")

    session.saveOrUpdate(dept1)
    session.saveOrUpdate(dept2)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student1", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student2", department = dept2, courseDepartment = dept2)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student3", department = dept2, courseDepartment = dept2)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student4", department = dept2, courseDepartment = dept2)

    stu1.groupName = "Undergraduate student"
    stu2.groupName = "Undergraduate student"
    stu3.groupName = "Undergraduate student"
    stu4.groupName = "Undergraduate student"

    stu1.mostSignificantCourse.levelCode = "1"
    stu2.mostSignificantCourse.levelCode = "2"
    stu3.mostSignificantCourse.levelCode = "2"
    stu4.mostSignificantCourse.levelCode = "3"

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.findUndergraduateUsercodesByHomeDepartmentAndLevel(dept1, "1") should contain only "student1"
    memberDao.findUndergraduateUsercodesByHomeDepartmentAndLevel(dept1, "2") should be(empty)
    memberDao.findUndergraduateUsercodesByHomeDepartmentAndLevel(dept2, "2") should contain allOf("student2", "student3")
    memberDao.findUndergraduateUsercodesByHomeDepartmentAndLevel(dept2, "3") should contain only "student4"
  }

  @Test
  def findFinalistUndergraduateUsercodes(): Unit = transactional { tx =>
    val dept1 = Fixtures.department("hm", "History of Music")
    session.saveOrUpdate(dept1)

    val stu1 = Fixtures.student(universityId = "1000001", userId = "student1", department = dept1, courseDepartment = dept1)
    val stu2 = Fixtures.student(universityId = "1000002", userId = "student2", department = dept1, courseDepartment = dept1)
    val stu3 = Fixtures.student(universityId = "1000003", userId = "student3", department = dept1, courseDepartment = dept1)
    val stu4 = Fixtures.student(universityId = "1000004", userId = "student4", department = dept1, courseDepartment = dept1)

    stu1.groupName = "Undergraduate - full-time"
    stu2.groupName = "Undergraduate - full-time"
    stu3.groupName = "Undergraduate - full-time"
    stu4.groupName = "Undergraduate - full-time"

    stu1.mostSignificantCourse.levelCode = "2"
    stu2.mostSignificantCourse.levelCode = "3"
    stu3.mostSignificantCourse.levelCode = "3"
    stu4.mostSignificantCourse.levelCode = "F"

    stu1.mostSignificantCourse.courseYearLength = 3
    stu2.mostSignificantCourse.courseYearLength = 3
    stu3.mostSignificantCourse.courseYearLength = 4
    stu4.mostSignificantCourse.courseYearLength = 1

    memberDao.saveOrUpdate(stu1)
    memberDao.saveOrUpdate(stu2)
    memberDao.saveOrUpdate(stu3)
    memberDao.saveOrUpdate(stu4)

    memberDao.findFinalistUndergraduateUsercodes() should contain only "student2"
  }

  @Test
  def testDeleteByUniversityIds(): Unit = transactional { tx =>
    val app1 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id1",
      userId = "id1",
      department = null
    )
    val app2 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id2",
      userId = "id2",
      department = null
    )
    val app3 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id3",
      userId = "id3",
      department = null
    )
    val app4 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id4",
      userId = "id4",
      department = null
    )

    session.saveOrUpdate(app1)
    session.saveOrUpdate(app2)
    session.saveOrUpdate(app3)
    session.saveOrUpdate(app4)

    memberDao.deleteByUniversityIds(Seq("id1"))
    memberDao.getAllByUserId("id1") should be(empty)

    memberDao.deleteByUniversityIds(Seq("id2", "id3"))

    memberDao.getAllByUserId("id2") should be(empty)
    memberDao.getAllByUserId("id3") should be(empty)
  }

  @Test
  def testGetMemberMissingBefore(): Unit = transactional { tx =>
    val student1 = Fixtures.member(
      userType = MemberUserType.Student,
      universityId = "id1",
      userId = "id1",
      department = null
    )
    student1.missingFromImportSince = new DateTime().minusMonths(3)

    val student2 = Fixtures.member(
      userType = MemberUserType.Student,
      universityId = "id2",
      userId = "id2",
      department = null
    )
    student2.missingFromImportSince = new DateTime().minusMonths(1)

    session.saveOrUpdate(student1)
    session.saveOrUpdate(student2)

    val studentsMissingBeforeTwoMonths = memberDao.getMissingBefore[StudentMember](missingSince = new DateTime().minusMonths(2))
    studentsMissingBeforeTwoMonths.size should be(1)
    studentsMissingBeforeTwoMonths.head should be("id1")

    val applicant1 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id3",
      userId = "id3",
      department = null
    )
    applicant1.missingFromImportSince = new DateTime().minusMonths(10)

    val applicant2 = Fixtures.member(
      userType = MemberUserType.Applicant,
      universityId = "id4",
      userId = "id4",
      department = null
    )
    applicant2.missingFromImportSince = new DateTime().minusMonths(3)

    session.saveOrUpdate(applicant1)
    session.saveOrUpdate(applicant2)

    val applicantsMissingBeforeTwoMonths = memberDao.getMissingBefore[ApplicantMember](missingSince = new DateTime().minusMonths(2))
    applicantsMissingBeforeTwoMonths.size should be(2)
    applicantsMissingBeforeTwoMonths should contain allOf("id3", "id4")

    val membersMissingBeforeTwoMonths = memberDao.getMissingBefore[Member](missingSince = new DateTime().minusMonths(2))
    membersMissingBeforeTwoMonths.size should be(3)
    membersMissingBeforeTwoMonths should contain allOf("id3", "id4", "id1")
  }

}

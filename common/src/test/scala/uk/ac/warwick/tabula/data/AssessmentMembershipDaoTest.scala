package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceImpl, ManualMembershipInfo}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, MockUserLookup, PersistenceTestBase}
import uk.ac.warwick.userlookup.User

class AssessmentMembershipDaoTest extends PersistenceTestBase {

  val dao = new AssessmentMembershipDaoImpl
  val assignmentMembershipService = new AssessmentMembershipServiceImpl
  assignmentMembershipService.dao = dao

  trait Fixture {
    val dept: Department = Fixtures.department("in")
    val module1: Module = Fixtures.module("in101")
    val module2: Module = Fixtures.module("in102")

    dept.modules.add(module1)
    dept.modules.add(module2)
    module1.adminDepartment = dept
    module2.adminDepartment = dept

    session.save(dept)
    session.save(module1)
    session.save(module2)

    val assignment1: Assignment = Fixtures.assignment("assignment 1")
    assignment1.assessmentMembershipService = assignmentMembershipService

    val assignment2: Assignment = Fixtures.assignment("assignment 2")
    assignment2.assessmentMembershipService = assignmentMembershipService

    val assignment3: Assignment = Fixtures.assignment("assignment 3")
    assignment3.assessmentMembershipService = assignmentMembershipService

    val assignment4: Assignment = Fixtures.assignment("assignment 4")
    assignment4.assessmentMembershipService = assignmentMembershipService

    val assignment5: Assignment = Fixtures.assignment("assignment 5")
    assignment5.assessmentMembershipService = assignmentMembershipService

    val resitAssignment1: Assignment = Fixtures.assignment("resit assignment 1")
    resitAssignment1.resitAssessment = true
    resitAssignment1.assessmentMembershipService = assignmentMembershipService
    val resitAssignment2: Assignment = Fixtures.assignment("resit assignment 2")
    resitAssignment2.resitAssessment = true
    resitAssignment2.assessmentMembershipService = assignmentMembershipService

    module1.assignments.add(assignment1)
    module1.assignments.add(assignment2)
    module1.assignments.add(resitAssignment1)
    module1.assignments.add(resitAssignment2)

    module2.assignments.add(assignment3)
    module2.assignments.add(assignment4)
    module2.assignments.add(assignment5)

    assignment1.module = module1
    assignment2.module = module1
    resitAssignment1.module = module1
    resitAssignment2.module = module1
    assignment3.module = module2
    assignment4.module = module2
    assignment5.module = module2

    // manually enrolled on assignment 1
    assignment1.members.knownType.addUserId("cuscav")

    // assessment component enrolment for assignment 2
    val assignment2AC = new AssessmentComponent
    assignment2AC.moduleCode = "in101-10"
    assignment2AC.assessmentGroup = "A"
    assignment2AC.sequence = "A02"
    assignment2AC.module = module1
    assignment2AC.assessmentType = AssessmentType.Essay
    assignment2AC.name = "Cool Essay"
    assignment2AC.inUse = true

    val assessmentGroup2 = new AssessmentGroup
    assessmentGroup2.membershipService = assignmentMembershipService
    assessmentGroup2.assessmentComponent = assignment2AC
    assessmentGroup2.occurrence = "A"
    assessmentGroup2.assignment = assignment2

    val upstreamGroup2 = new UpstreamAssessmentGroup
    upstreamGroup2.moduleCode = "in101-10"
    upstreamGroup2.occurrence = "A"
    upstreamGroup2.assessmentGroup = "A"
    upstreamGroup2.sequence = "A02"
    upstreamGroup2.academicYear = AcademicYear(2010)
    upstreamGroup2.members = JArrayList(
      new UpstreamAssessmentGroupMember(upstreamGroup2, "0672089"),
      new UpstreamAssessmentGroupMember(upstreamGroup2, "1000005")
    )

    assignment2.assessmentGroups.add(assessmentGroup2)
    assignment2.academicYear = AcademicYear(2010)

    session.save(assignment2AC)
    session.save(upstreamGroup2)

    // assessment component enrolment for assignment 3 AND manually enrolled
    val assignment3AC = new AssessmentComponent
    assignment3AC.moduleCode = "in102-10"
    assignment3AC.assessmentGroup = "A"
    assignment3AC.sequence = "A01"
    assignment3AC.module = module2
    assignment3AC.assessmentType = AssessmentType.Essay
    assignment3AC.name = "Cool Stuff"
    assignment3AC.inUse = true

    val assessmentGroup3 = new AssessmentGroup
    assessmentGroup3.membershipService = assignmentMembershipService
    assessmentGroup3.assessmentComponent = assignment3AC
    assessmentGroup3.occurrence = "A"
    assessmentGroup3.assignment = assignment3

    val upstreamGroup3 = new UpstreamAssessmentGroup
    upstreamGroup3.moduleCode = "in102-10"
    upstreamGroup3.occurrence = "A"
    upstreamGroup3.assessmentGroup = "A"
    upstreamGroup3.academicYear = AcademicYear(2010)
    upstreamGroup3.members = JArrayList(new UpstreamAssessmentGroupMember(upstreamGroup3, "0672089"))

    assignment3.assessmentGroups.add(assessmentGroup3)
    assignment3.academicYear = AcademicYear(2010)
    assignment3.members.knownType.addUserId("cuscav")

    session.save(assignment3AC)
    session.save(upstreamGroup3)

    // assessment component enrolment for assignment 4 but manually excluded
    val assessmentGroup4 = new AssessmentGroup
    assessmentGroup4.membershipService = assignmentMembershipService
    assessmentGroup4.assessmentComponent = assignment3AC
    assessmentGroup4.occurrence = "A"
    assessmentGroup4.assignment = assignment4

    assignment4.assessmentGroups.add(assessmentGroup4)
    assignment4.academicYear = AcademicYear(2010)
    assignment4.members.knownType.excludeUserId("cuscav")

    val user = new User("cuscav")
    user.setWarwickId("0672089")

    val student: StudentMember = new StudentMember("0672089")
    student.userId = "cuscav"

    val studentCourseDetails = new StudentCourseDetails(student, "0672089/1")
    studentCourseDetails.sprCode = "0672089/1"
    studentCourseDetails.statusOnCourse = new SitsStatus(code = "C")

    val studentCourseYearDetails = new StudentCourseYearDetails(studentCourseDetails, 1, AcademicYear(2010))
    studentCourseDetails.addStudentCourseYearDetails(studentCourseYearDetails)

    session.saveOrUpdate(student)
    session.saveOrUpdate(studentCourseDetails)
    session.saveOrUpdate(studentCourseYearDetails)

    val userLookup = new MockUserLookup
    userLookup.registerUserObjects(user)

    assignmentMembershipService.userLookup = userLookup
    assignmentMembershipService.assignmentManualMembershipHelper.userLookup = userLookup
  }

  @Before def setup(): Unit = {
    dao.sessionFactory = sessionFactory
    assignmentMembershipService.assignmentManualMembershipHelper.sessionFactory = sessionFactory
    assignmentMembershipService.assignmentManualMembershipHelper.cache.foreach(_.clear())
  }

  @Test def enrolledAssignments(): Unit = {
    transactional { _ =>
      new Fixture {
        session.save(assignment2AC)
        session.save(upstreamGroup2)
        session.flush()
        session.save(assignment3AC)
        session.save(upstreamGroup3)
        session.flush()
        session.save(dept)
        session.flush()

        assignmentMembershipService.getEnrolledAssignments(user, None).toSet should be(Set(assignment1, assignment2, assignment3))
      }
    }
  }

  @Test def enrolledResitAssignmentsWithResitMemeber(): Unit = {
    transactional { _ =>
      new Fixture {

        // assessment component enrolment for resitAssignment 1
        val resitAssignment1AC = new AssessmentComponent
        resitAssignment1AC.moduleCode = "in101-20"
        resitAssignment1AC.assessmentGroup = "A1"
        resitAssignment1AC.sequence = "A03"
        resitAssignment1AC.module = module1
        resitAssignment1AC.assessmentType = AssessmentType.Essay
        resitAssignment1AC.name = "Resit Essay"
        resitAssignment1AC.inUse = true

        val resitAssessmentGroup1 = new AssessmentGroup
        resitAssessmentGroup1.membershipService = assignmentMembershipService
        resitAssessmentGroup1.assessmentComponent = resitAssignment1AC
        resitAssessmentGroup1.occurrence = "A"
        resitAssessmentGroup1.assignment = resitAssignment1

        // upstream group with some student marked as resit
        val upstreamGroup5WithResitStudent = new UpstreamAssessmentGroup
        upstreamGroup5WithResitStudent.moduleCode = "in101-20"
        upstreamGroup5WithResitStudent.occurrence = "A"
        upstreamGroup5WithResitStudent.assessmentGroup = "A1"
        upstreamGroup5WithResitStudent.sequence = "A03"
        upstreamGroup5WithResitStudent.academicYear = AcademicYear(2010)
        val uagMember1 = new UpstreamAssessmentGroupMember(upstreamGroup5WithResitStudent, "0672089")
        uagMember1.resitExpected = Some(true)
        val uagMember2 = new UpstreamAssessmentGroupMember(upstreamGroup5WithResitStudent, "1000005")
        uagMember2.resitExpected = Some(false)

        upstreamGroup5WithResitStudent.members = JArrayList(
          uagMember1, uagMember2
        )

        resitAssignment1.assessmentGroups.add(resitAssessmentGroup1)
        resitAssignment1.academicYear = AcademicYear(2010)
        session.save(resitAssignment1AC)
        session.save(upstreamGroup5WithResitStudent)
        session.flush()
        session.save(assignment2AC)
        session.save(upstreamGroup2)
        session.flush()
        session.save(assignment3AC)
        session.save(upstreamGroup3)
        session.flush()
        session.save(dept)
        session.flush()

        assignmentMembershipService.getEnrolledAssignments(user, None).toSet should be(Set(assignment1, assignment2, assignment3, resitAssignment1))
      }
    }
  }


  @Test def enrolledResitAssignmentsWithNonResitMember(): Unit = {
    transactional { _ =>
      new Fixture {
        // assessment component enrolment for resitAssignment 2
        val resitAssignment2AC = new AssessmentComponent
        resitAssignment2AC.moduleCode = "in101-20"
        resitAssignment2AC.assessmentGroup = "A1"
        resitAssignment2AC.sequence = "A04"
        resitAssignment2AC.module = module1
        resitAssignment2AC.assessmentType = AssessmentType.Essay
        resitAssignment2AC.name = "Resit Essay-01"
        resitAssignment2AC.inUse = true

        val resitAssessmentGroup2 = new AssessmentGroup
        resitAssessmentGroup2.membershipService = assignmentMembershipService
        resitAssessmentGroup2.assessmentComponent = resitAssignment2AC
        resitAssessmentGroup2.occurrence = "A"
        resitAssessmentGroup2.assignment = resitAssignment1

        //includes a student that doesn't have resit assignment
        val upstreamGroup6WithoutResit = new UpstreamAssessmentGroup
        upstreamGroup6WithoutResit.moduleCode = "in101-20"
        upstreamGroup6WithoutResit.occurrence = "A"
        upstreamGroup6WithoutResit.assessmentGroup = "A1"
        upstreamGroup6WithoutResit.sequence = "A04"
        upstreamGroup6WithoutResit.academicYear = AcademicYear(2010)
        val uagMember11 = new UpstreamAssessmentGroupMember(upstreamGroup6WithoutResit, "0672089")
        uagMember11.resitExpected = Some(false)

        upstreamGroup6WithoutResit.members = JArrayList(uagMember11)

        resitAssignment2.assessmentGroups.add(resitAssessmentGroup2)
        resitAssignment2.academicYear = AcademicYear(2010)
        session.save(resitAssignment2AC)
        session.save(upstreamGroup6WithoutResit)
        session.flush()
        session.save(assignment2AC)
        session.save(upstreamGroup2)
        session.flush()
        session.save(assignment3AC)
        session.save(upstreamGroup3)
        session.flush()
        session.save(dept)
        session.flush()

        assignmentMembershipService.getEnrolledAssignments(user, None).toSet should be(Set(assignment1, assignment2, assignment3))
      }
    }
  }


  /** TAB-1824 if uniid appears twice in upstream group users, SQL sadness can result. */
  @Test def duplicateImportedUser(): Unit = {
    transactional { _ =>
      new Fixture {
        // Add user again
        upstreamGroup3.members = JArrayList(
          new UpstreamAssessmentGroupMember(upstreamGroup3, "0672089"),
          new UpstreamAssessmentGroupMember(upstreamGroup3, "0672089")
        )

        session.save(assignment2AC)
        session.save(upstreamGroup2)
        session.flush()
        session.save(assignment3AC)
        session.save(upstreamGroup3)
        session.flush()
        session.save(dept)
        session.flush()

        assignmentMembershipService.getEnrolledAssignments(user, None).toSet should be(Set(assignment1, assignment2, assignment3))
      }
    }
  }

  @Test def departmentsWithManualAssessmentsOrGroups(): Unit = {
    transactional { _ =>
      new Fixture {
        val thisYear: AcademicYear = AcademicYear.now()

        assignment1.members.knownType.removeUserId("cuscav")
        session.save(assignment1)
        session.flush()

        assignmentMembershipService.departmentsWithManualAssessmentsOrGroups(thisYear) should be(Seq())
      }
    }
  }

  @Test def departmentsManualMembership(): Unit = {
    transactional { _ =>
      new Fixture {

        def testMember(id: String): StudentMember = {
          val m = new StudentMember(id)
          m.userId = id
          m
        }

        val members: Seq[StudentMember] = Seq(testMember("cuscav"), testMember("cuslaj"), testMember("cuslat"))
        members.foreach(session.save)
        session.flush()

        assignment4.members.knownType.addUserId("cuscav")
        session.save(assignment4)
        assignment5.members.knownType.addUserId("cuscav")
        assignment5.members.knownType.addUserId("cuslaj")
        assignment5.members.knownType.addUserId("cuslat")
        session.save(assignment5)
        session.flush()

        val thisYear: AcademicYear = AcademicYear.now()
        val membershipInfo: ManualMembershipInfo = assignmentMembershipService.departmentsManualMembership(dept, thisYear)

        membershipInfo.assignments.toSet should be(Set(assignment1, assignment5))

        assignment1.members.knownType.removeUserId("cuscav")
        session.save(assignment1)
        session.flush()

        val membershipInfo2: ManualMembershipInfo = assignmentMembershipService.departmentsManualMembership(dept, thisYear)
        membershipInfo2.assignments should be (Seq(assignment5))
      }
    }
  }

  @Test def UpstreamAssessmentGroupMembers(): Unit = {
    transactional { _ =>
      new Fixture {
        val academicYear: AcademicYear = AcademicYear.now()

        val dept1 = Fixtures.department("its")
        val sprFullyEnrolledStatus: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")
        val sprPWDStatus: SitsStatus = Fixtures.sitsStatus("P", "PWD", "Permanently Withdrawn")
        session.saveOrUpdate(dept1)
        session.saveOrUpdate(sprFullyEnrolledStatus)
        session.flush()

        val stu1 = Fixtures.student("1000005", "1000005", department = dept1, sprStatus = sprFullyEnrolledStatus)
        val stu2: StudentMember = Fixtures.student("1000006", "u1000006", department = dept)
        stu1.mostSignificantCourse.statusOnCourse = sprFullyEnrolledStatus
        stu2.mostSignificantCourse.statusOnCourse = sprPWDStatus
        session.save(stu1)
        session.save(stu2)
        upstreamGroup2.academicYear = academicYear
        session.update(upstreamGroup2)
        session.flush()
        val uagInfo: Seq[UpstreamAssessmentGroupInfo] = assignmentMembershipService.getUpstreamAssessmentGroupInfo(assignment2AC, academicYear)
        uagInfo.size should be(1)
        val currentMembers: Seq[UpstreamAssessmentGroupMember] = uagInfo.head.currentMembers
        currentMembers.size should be(1)

        //now add one more PWD member. We should still have just 1 member
        upstreamGroup2.members.add(new UpstreamAssessmentGroupMember(upstreamGroup2, "1000006"))
        session.update(upstreamGroup2)
        session.flush()

        val uagInfo1: Seq[UpstreamAssessmentGroupInfo] = assignmentMembershipService.getUpstreamAssessmentGroupInfo(assignment2AC, academicYear)
        uagInfo1.size should be(1)
        val currentMembers1: Seq[UpstreamAssessmentGroupMember] = uagInfo1.head.currentMembers
        currentMembers1.size should be(1)

        //add one more fully enrolled member.
        val stu3: StudentMember = Fixtures.student("0000007", "u1000007", department = dept)
        stu3.mostSignificantCourse.statusOnCourse = sprFullyEnrolledStatus
        session.save(stu3)
        session.flush()

        upstreamGroup2.members.add(new UpstreamAssessmentGroupMember(upstreamGroup2, "0000007"))
        session.update(upstreamGroup2)
        session.flush()

        val uagInfo2: Seq[UpstreamAssessmentGroupInfo] = assignmentMembershipService.getUpstreamAssessmentGroupInfo(assignment2AC, academicYear)
        val currentMembers2: Seq[UpstreamAssessmentGroupMember] = uagInfo2.filter(_.upstreamAssessmentGroup == upstreamGroup2).head.currentMembers
        currentMembers2.size should be(2)

        currentMembers2.foreach { uagm =>
          uagm.universityId should not be "1000006"
        }

        val uag: Seq[UpstreamAssessmentGroupInfo] = assignmentMembershipService.getUpstreamAssessmentGroupInfo(Seq(assessmentGroup2, assessmentGroup3, assessmentGroup4), academicYear)
        uag should be(uagInfo2)
      }
    }
  }

}

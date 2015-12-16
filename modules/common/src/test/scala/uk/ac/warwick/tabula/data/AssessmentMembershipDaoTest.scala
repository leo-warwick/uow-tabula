package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{MockUserLookup, PersistenceTestBase, Fixtures, AcademicYear, MockGroupService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.AssessmentComponent
import uk.ac.warwick.tabula.data.model.AssessmentType
import uk.ac.warwick.tabula.data.model.AssessmentGroup
import uk.ac.warwick.tabula.services.AssessmentMembershipServiceImpl
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import org.junit.Before

class AssessmentMembershipDaoTest extends PersistenceTestBase {

	val dao = new AssessmentMembershipDaoImpl
	val assignmentMembershipService = new AssessmentMembershipServiceImpl
	assignmentMembershipService.dao = dao

	trait Fixture {
		val dept = Fixtures.department("in")

		val module1 = Fixtures.module("in101")
		val module2 = Fixtures.module("in102")

		dept.modules.add(module1)
		dept.modules.add(module2)

		session.save(dept)
		session.save(module1)
		session.save(module2)

		val assignment1 = Fixtures.assignment("assignment 1")
		assignment1.assessmentMembershipService = assignmentMembershipService

		val assignment2 = Fixtures.assignment("assignment 2")
		assignment2.assessmentMembershipService = assignmentMembershipService

		val assignment3 = Fixtures.assignment("assignment 3")
		assignment3.assessmentMembershipService = assignmentMembershipService

		val assignment4 = Fixtures.assignment("assignment 4")
		assignment4.assessmentMembershipService = assignmentMembershipService

		module1.assignments.add(assignment1)
		module1.assignments.add(assignment2)
		module2.assignments.add(assignment3)
		module2.assignments.add(assignment4)

		// manually enrolled on assignment 1
		assignment1.members.knownType.addUserId("cuscav")

		// assessment component enrolment for assignment 2
		val assignment2AC = new AssessmentComponent
		assignment2AC.moduleCode = "in101-10"
		assignment2AC.assessmentGroup = "A"
		assignment2AC.sequence = "A02"
		assignment2AC.module = module1
		assignment2AC.assessmentType = AssessmentType.Assignment
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
		upstreamGroup2.academicYear = new AcademicYear(2010)
		upstreamGroup2.members.knownType.staticUserIds = Seq("0672089")

		assignment2.assessmentGroups.add(assessmentGroup2)
		assignment2.academicYear = new AcademicYear(2010)

		session.save(assignment2AC)
		session.save(upstreamGroup2)

		// assessment component enrolment for assignment 3 AND manually enrolled
		val assignment3AC = new AssessmentComponent
		assignment3AC.moduleCode = "in102-10"
		assignment3AC.assessmentGroup = "A"
		assignment3AC.sequence = "A01"
		assignment3AC.module = module2
		assignment3AC.assessmentType = AssessmentType.Assignment
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
		upstreamGroup3.academicYear = new AcademicYear(2010)
		upstreamGroup3.members.knownType.staticUserIds = Seq("0672089")

		assignment3.assessmentGroups.add(assessmentGroup3)
		assignment3.academicYear = new AcademicYear(2010)
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
		assignment4.academicYear = new AcademicYear(2010)
		assignment4.members.knownType.excludeUserId("cuscav")

		val user = new User("cuscav")
		user.setWarwickId("0672089")

		val userLookup = new MockUserLookup
		userLookup.registerUserObjects(user)

		assignmentMembershipService.userLookup = userLookup
		assignmentMembershipService.assignmentManualMembershipHelper.userLookup = userLookup
	}

	@Before def setup() {
		dao.sessionFactory = sessionFactory
		assignmentMembershipService.assignmentManualMembershipHelper.sessionFactory = sessionFactory
		assignmentMembershipService.assignmentManualMembershipHelper.cache.foreach { _.clear() }
	}

	@Test def getEnrolledAssignments() {
		transactional { tx =>
			new Fixture {
				session.save(assignment2AC)
				session.save(upstreamGroup2)
				session.flush
				session.save(assignment3AC)
				session.save(upstreamGroup3)
				session.flush
				session.save(dept)
				session.flush

				assignmentMembershipService.getEnrolledAssignments(user).toSet should be (Set(assignment1, assignment2, assignment3))
			}
		}
	}

	/** TAB-1824 if uniid appears twice in upstream group users, SQL sadness can result. */
	@Test def duplicateImportedUser() {
		transactional { tx =>
			new Fixture {
				// Add user again
				upstreamGroup3.members.knownType.staticUserIds = Seq("0672089", "0672089")

				session.save(assignment2AC)
				session.save(upstreamGroup2)
				session.flush
				session.save(assignment3AC)
				session.save(upstreamGroup3)
				session.flush
				session.save(dept)
				session.flush

				assignmentMembershipService.getEnrolledAssignments(user).toSet should be (Set(assignment1, assignment2, assignment3))
			}
		}
	}

}

package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures, AcademicYear}
import org.junit.Before
import uk.ac.warwick.tabula.data.AssignmentDaoImpl
import org.joda.time.DateTime

class AssignmentDaoTest extends PersistenceTestBase {

	val dao = new AssignmentDaoImpl

	trait Fixture {
		val dept = Fixtures.department("in")
		session.save(dept)

		val module1InDept = Fixtures.module("in101")
		module1InDept.department = dept

		val module2InDept = Fixtures.module("in102")
		module2InDept.department = dept

		session.save(module1InDept)
		session.save(module2InDept)

		dept.modules.add(module1InDept)
		dept.modules.add(module2InDept)

		val moduleNotInDept = Fixtures.module("ca101")
		session.save(moduleNotInDept)

		val thisYear = AcademicYear.guessByDate(new DateTime())
		val previousYear = AcademicYear.guessByDate(new DateTime().minusYears(2))

		// these assignments are in the current department and year
		val assignment1 = Fixtures.assignment("assignment 1")
		assignment1.module = module1InDept //this does the necessary whereas this doesn't: module1InDept.assignments.add(assignment1)
		assignment1.academicYear = thisYear
		assignment1.createdDate = new DateTime()

		val assignment2 = Fixtures.assignment("assignment 2")
		assignment2.module = module2InDept
		assignment2.academicYear = thisYear
		// TAB-2459 - ensure assignment1 is the most recent assignment
		assignment2.createdDate = new DateTime().minusMinutes(1)

		// assignment in wrong dept
		val assignment3 = Fixtures.assignment("assignment 3")
		assignment3.module = moduleNotInDept
		assignment3.academicYear = thisYear
		assignment3.createdDate = new DateTime().minusMinutes(1)

		// assignment in wrong year
		val assignment4 = Fixtures.assignment("assignment 4")
		module1InDept.assignments.add(assignment4)
		assignment4.academicYear = previousYear
		assignment3.createdDate = new DateTime().minusMinutes(1)

		session.save(assignment1)
		session.save(assignment2)
		session.save(assignment3)
		session.save(assignment4)

		session.flush()
		session.clear()

	}

	@Before def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def everythingPersisted {
		transactional { tx =>
			new Fixture {
				assignment1.id should not be (null)
				assignment2.id should not be (null)
				assignment3.id should not be (null)
				assignment4.id should not be (null)
				module1InDept.id should not be (null)
				module2InDept.id should not be (null)
				moduleNotInDept.id should not be (null)
				dept.id should not be (null)
				module1InDept.department should be (dept)
				module2InDept.department should be (dept)
				assignment1.module should be (module1InDept)
				assignment2.module should be (module2InDept)
			}
		}
	}

	@Test def getAssignmentsByName {
		transactional { tx =>
			new Fixture {
				val assignments = dao.getAssignmentsByName("assignment 1", dept)
				assignments.size should be (1)
			}
		}
	}

	@Test def getRecentAssignment {
		transactional { tx =>
			new Fixture {
				val assignment = dao.recentAssignment(dept)
				assignment.get.name should be ("assignment 1")
			}
		}
	}

	@Test def getAssignments {
		transactional { tx =>
			new Fixture {
				val assignments = dao.getAssignments(dept, thisYear)
				assignments.size should be (2)
				assignments.contains(assignment1) should be (true)
				assignments.contains(assignment2) should be (true)
				assignments.contains(assignment3) should be (false)
				assignments.contains(assignment4) should be (false)
			}
		}
	}
}

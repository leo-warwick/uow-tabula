package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import org.junit.Before
import uk.ac.warwick.tabula.data.AssessmentDaoImpl
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, PersistenceTestBase}

class AssessmentDaoTest extends PersistenceTestBase {

	val dao = new AssessmentDaoImpl

	trait Fixture {
		val dept: Department = Fixtures.department("in")
		session.save(dept)

		val module1InDept: Module = Fixtures.module("in101")
		module1InDept.adminDepartment = dept

		val module2InDept: Module = Fixtures.module("in102")
		module2InDept.adminDepartment = dept

		session.save(module1InDept)
		session.save(module2InDept)

		dept.modules.add(module1InDept)
		dept.modules.add(module2InDept)

		val moduleNotInDept: Module = Fixtures.module("ca101")
		session.save(moduleNotInDept)

		val thisYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())
		val previousYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime().minusYears(2))

		// these assignments are in the current department and year
		val assignment1: Assignment = Fixtures.assignment("assignment 1")
		assignment1.module = module1InDept //this does the necessary whereas this doesn't: module1InDept.assignments.add(assignment1)
		assignment1.academicYear = thisYear
		assignment1.createdDate = new DateTime()

		val assignment2: Assignment = Fixtures.assignment("assignment 2")
		assignment2.module = module2InDept
		assignment2.academicYear = thisYear
		// TAB-2459 - ensure assignment1 is the most recent assignment
		assignment2.createdDate = new DateTime().minusMinutes(1)

		// assignment in wrong dept
		val assignment3: Assignment = Fixtures.assignment("assignment 3")
		assignment3.module = moduleNotInDept
		assignment3.academicYear = thisYear
		assignment3.createdDate = new DateTime().minusMinutes(1)

		// assignment in wrong year
		val assignment4: Assignment = Fixtures.assignment("assignment 4")
		module1InDept.assignments.add(assignment4)
		assignment4.academicYear = previousYear
		assignment3.createdDate = new DateTime().minusMinutes(1)

		session.save(assignment1)
		session.save(assignment2)
		session.save(assignment3)
		session.save(assignment4)

		val examModule: Module = Fixtures.module("abc123")
		session.save(examModule)

		val exam1_2013: Exam = Fixtures.exam("exam1")
		exam1_2013.module = examModule
		exam1_2013.academicYear = new AcademicYear(2013)

		val exam1_2014: Exam = Fixtures.exam("exam1")
		exam1_2014.module = examModule
		exam1_2014.academicYear = new AcademicYear(2014)

		val exam1_2015: Exam = Fixtures.exam("exam1")
		exam1_2015.module = examModule
		exam1_2015.academicYear = new AcademicYear(2015)

		session.save(exam1_2013)
		session.save(exam1_2014)
		session.save(exam1_2015)

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
				module1InDept.adminDepartment should be (dept)
				module2InDept.adminDepartment should be (dept)
				assignment1.module should be (module1InDept)
				assignment2.module should be (module2InDept)
			}
		}
	}

	@Test def getAssignmentsByName {
		transactional { tx =>
			new Fixture {
				val assignments: Seq[Assignment] = dao.getAssignmentsByName("assignment 1", dept)
				assignments.size should be (1)
			}
		}
	}

	@Test def getRecentAssignment {
		transactional { tx =>
			new Fixture {
				val assignment: Option[Assignment] = dao.recentAssignment(dept)
				assignment.get.name should be ("assignment 1")
			}
		}
	}

	@Test def getAssignments {
		transactional { tx =>
			new Fixture {
				val assignments: Seq[Assignment] = dao.getAssignments(dept, thisYear)
				assignments.size should be (2)
				assignments.contains(assignment1) should be (true)
				assignments.contains(assignment2) should be (true)
				assignments.contains(assignment3) should be (false)
				assignments.contains(assignment4) should be (false)
			}
		}
	}

	@Test def getExams: Unit = {
		transactional { tx =>
			new Fixture {
				val exams: Seq[Exam] = dao.getExamByNameYearModule("exam1", new AcademicYear(2014), examModule)
				exams.size should be (1)
				exams.contains(exam1_2013) should be (false)
				exams.contains(exam1_2014) should be (true)
				exams.contains(exam1_2015) should be (false)
			}
		}
	}
}

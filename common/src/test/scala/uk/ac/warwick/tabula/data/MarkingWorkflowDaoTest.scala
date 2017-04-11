package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before

class MarkingWorkflowDaoTest extends PersistenceTestBase {

	val dao = new MarkingWorkflowDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def crud() = transactional { tx =>
		val dept = Fixtures.department("in")
		session.save(dept)
		session.flush()

		val mw1 = Fixtures.seenSecondMarkingLegacyWorkflow("mw1")
		mw1.department = dept

		val mw2 = Fixtures.studentsChooseMarkerWorkflow("mw2")
		mw2.department = dept

		val ass1 = Fixtures.assignment("ass1")
		val ass2 = Fixtures.assignment("ass2")

		session.save(ass1)
		session.save(ass2)
		session.save(mw1)
		session.save(mw2)
		session.flush()

		dao.getAssignmentsUsingMarkingWorkflow(mw1) should be ('empty)
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be ('empty)

		ass1.markingWorkflow = mw1
		ass2.markingWorkflow = mw1

		session.save(ass1)
		session.save(ass2)

		dao.getAssignmentsUsingMarkingWorkflow(mw1).toSet should be (Seq(ass1, ass2).toSet)
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be ('empty)

		ass2.markingWorkflow = mw2

		session.save(ass2)

		dao.getAssignmentsUsingMarkingWorkflow(mw1) should be (Seq(ass1))
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be (Seq(ass2))
	}

}

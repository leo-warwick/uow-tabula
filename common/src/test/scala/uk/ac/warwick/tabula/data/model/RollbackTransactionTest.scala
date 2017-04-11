package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures, AppContextTestBase}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoImpl, MemberDaoImpl, MemberDao}
import org.junit.Before

/**
 * This test exists to prove that our TransactionalTesting.transactional(){} function rolls back transactions
 * (or otherwise cleans up the database state) in between each test method
 *
 */
class RollbackTransactionTest  extends PersistenceTestBase{

	val dao = new AutowiringMemberDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test
	def insertOneThing()= transactional{tx=>

		dao.getAllByUserId("student") should be(Nil)

		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		dao.saveOrUpdate(m1)
		session.flush()

		dao.getAllByUserId("student") should not be(Nil)

	}

	@Test
	def insertOneThingAgain()= transactional{tx=>

			dao.getAllByUserId("student") should be(Nil)

		val m1 = Fixtures.student(universityId = "0000001", userId="student")
		dao.saveOrUpdate(m1)
		session.flush()

		dao.getAllByUserId("student") should not be(Nil)

	}
}

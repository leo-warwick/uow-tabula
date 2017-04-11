package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}
import org.junit.Before
import org.joda.time.DateTime.now
import uk.ac.warwick.tabula.data.model.Module

class ModuleDaoTest extends PersistenceTestBase {

	val dao = new ModuleDaoImpl

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	trait Context {
		// Already inserted by data.sql
		val cs108: Module = dao.getByCode("cs108").get
		val cs240: Module = dao.getByCode("cs240").get
		val cs241: Module = dao.getByCode("cs241").get
		val cs242: Module = dao.getByCode("cs242").get
	}

	@Test def crud { transactional { tx =>
		new Context {
			dao.allModules should be (Seq(cs108, cs240, cs241, cs242))

			val cs333: Module = Fixtures.module("cs333")
			dao.saveOrUpdate(cs333)

			dao.allModules should be (Seq(cs108, cs240, cs241, cs242, cs333))

			dao.getByCode("cs333") should be (Some(cs333))
			dao.getByCode("wibble") should be (None)

			dao.getById(cs108.id) should be (Some(cs108))
			dao.getById(cs333.id) should be (Some(cs333))
			dao.getById("wibble") should be (None)

			dao.findModulesNamedLike("Cs") should be (Seq(cs108, cs240, cs241, cs242, cs333))
			dao.findModulesNamedLike("s2") should be (Seq(cs240, cs241, cs242))
			dao.findModulesNamedLike("Hello") should be (Seq())

			dao.hasAssignments(cs108) should be (true)
			dao.hasAssignments(cs242) should be (false)

		}
	}}

	@Test
	def testStampMissingFromImport = transactional { tx =>
		val module1 = Fixtures.module("one", "my name is one") // not stale before, stale now
		val module2 = Fixtures.module("two", "my name is two") // not stale before, not stale now
		val module3 = Fixtures.module("three", "my name is three")

		val previously = now.minusDays(3)
		module3.missingFromImportSince = previously // stale before, not stale now
		val module4 = Fixtures.module("four", "my name is four")

		module4.missingFromImportSince = previously // stale before, stale now

		session.saveOrUpdate(module1)
		session.saveOrUpdate(module2)
		session.saveOrUpdate(module3)
		session.saveOrUpdate(module4)

		dao.getByCode("one").get.missingFromImportSince should be (null)
		dao.getByCode("two").get.missingFromImportSince should be (null)
		dao.getByCode("three").get.missingFromImportSince should be (previously)
		dao.getByCode("four").get.missingFromImportSince should be (previously)

		val staleCodes = Seq[String]("one", "four")

		dao.stampMissingFromImport(staleCodes)
		session.flush
		session.clear

		// check module 1 has just been stamped
		dao.getByCode("one").get.missingFromImportSince.isAfter(now.minusMinutes(5)) should be (true)
		dao.getByCode("one").get.missingFromImportSince.isBefore(now.plusMinutes(5)) should be (true)

		// module 2 is not missing
		dao.getByCode("two").get.missingFromImportSince should be (null)

		// module 3 is not newly stale but the dao stamper doesn't nullify existing stamps
		dao.getByCode("three").get.missingFromImportSince should not be (null)

		// module 4 has been found to be stale on this import, but needs to retain its old stamp
		dao.getByCode("four").get.missingFromImportSince should be (previously)

	}

}
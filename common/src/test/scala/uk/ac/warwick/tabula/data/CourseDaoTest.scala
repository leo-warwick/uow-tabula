package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.Fixtures

class CourseDaoTest extends PersistenceTestBase {

	@Test def crud = transactional { tx =>
		val dao = new CourseDaoImpl
		dao.sessionFactory = sessionFactory
		val course = Fixtures.course("TPOS-M9P0")
		dao.saveOrUpdate(course)

		dao.getByCode("TPOS-M9P0") should be (Some(course))
		dao.getByCode("TPOS-M9P0").get.name should be ("Course TPOS-M9P0")

		dao.getByCode("wibble") should be (None)
	}

}
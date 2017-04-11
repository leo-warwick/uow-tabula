package uk.ac.warwick.tabula.data

import org.junit.Before
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}

class RouteDaoTest extends PersistenceTestBase {

	val dao = new RouteDaoImpl

	val route: Route = Fixtures.route("g553")

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
	}

	@Test def byCode():Unit = transactional { tx =>
		dao.saveOrUpdate(route)

		dao.getByCode("g553") should be (Some(route))
		dao.getByCode("wibble") should be (None)
	}
}
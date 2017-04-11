package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.RequestInfo

class RequestLevelCachingTest extends TestBase {

	val cache = new RequestLevelCaching[String, String] {}

	@Test def withoutRequest {
		var timesRun = 0
		def expensiveOp = {
			timesRun += 1
			"solution"
		}

		cache.cachedBy("key") { expensiveOp } should be ("solution")
		timesRun should be (1)

		cache.cachedBy("key") { expensiveOp } should be ("solution")
		timesRun should be (2)

		cache.cache should be ('empty)
	}

	@Test def withRequest {
		var timesRun = 0
		def expensiveOp() = {
			timesRun += 1
			"solution"
		}

		withUser("cuscav") {
			cache.cachedBy("key") { expensiveOp } should be ("solution")
			timesRun should be (1)

			cache.cachedBy("key") { expensiveOp } should be ("solution")
			timesRun should be (1)

			cache.cachedBy("other-key") { expensiveOp } should be ("solution")
			timesRun should be (2)

			cache.cachedBy("other-key") { expensiveOp } should be ("solution")
			timesRun should be (2)

			cache.cachedBy("key") { expensiveOp } should be ("solution")
			timesRun should be (2)

			cache.cache should be (Some(Map("key" -> "solution", "other-key" -> "solution")))
		}

		// Out of request, cache should be empty again
		cache.cache should be ('empty)

		cache.cachedBy("key") { expensiveOp } should be ("solution")
		timesRun should be (3)

		withUser("cuscav") {
			cache.cachedBy("key") { expensiveOp } should be ("solution")
			timesRun should be (4)

			cache.cachedBy("key") { expensiveOp } should be ("solution")
			timesRun should be (4)

			cache.cache should be (Some(Map("key" -> "solution")))
		}
	}

}
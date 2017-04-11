package uk.ac.warwick.tabula

class JavaImportsTest extends TestBase with JavaImports {

	@Test def jSet() {
		JSet("a", "b", "b").size() should be (2)
	}

	@Test def jBoolean() {
		ToJBoolean(Some(true)) should be (java.lang.Boolean.TRUE)
		ToJBoolean(Some(false)) should be (java.lang.Boolean.FALSE)
		ToJBoolean(None) should be (null)
	}

	@Test def jInteger() {
		ToJInteger(Some(1)) should be (1)
		ToJInteger(Some(0)) should be (0)
		ToJInteger(None) should be (null)
	}

	@Test def concurrentHashMap() {
		val map = JConcurrentHashMap[String, Int]()
		map.put("1", 1)

		map.getOrElseUpdate("1", 2) should be (1)
		map.getOrElseUpdate("2", 2) should be (2)

		// Simulate a put in another thread
		map.getOrElseUpdate("3", {
			map.put("3", 3)
			4 // this value is thrown away
		}) should be (3)
	}

}
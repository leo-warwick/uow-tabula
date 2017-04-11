package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase

class ProductsTest extends TestBase with Products {

	@Test def itWorks {
		val pairs = (Map(
				"a" -> null,
				"d" -> "e",
				"b" -> "c"
		) + ((null, "steve"))).toSeq

		pairs.filterNot(nullKey).toMap should be (Map(
				"a" -> null,
				"d" -> "e",
				"b" -> "c"
		))

		pairs.filterNot(nullValue).toMap should be ((Map(
				"d" -> "e",
				"b" -> "c"
		) + ((null, "steve"))))

		pairs.filterNot(nullKey).filterNot(nullValue).sortBy(toKey) should be (Seq(("b", "c"), ("d", "e")))
		pairs.filterNot(nullKey).filterNot(nullValue).sortBy(toKey).map(toValue) should be (Seq("c", "e"))
	}

}
package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase

// scalastyle:off magic.number
class LazyMapsTest extends TestBase {

	@Test def lazyMap {
		var valuesCalculated = 0
		val map = LazyMaps.create{ (i:Int) =>
			valuesCalculated += 1
			"String value %d" format (i)
		}

		map(3) should be ("String value 3")
		map(20) should be ("String value 20")
		map(20) should be ("String value 20")
		valuesCalculated should be (2)
	}

}
package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase

class PhoneNumberFormatterTest extends TestBase {
	import PhoneNumberFormatter.format

	@Test def local {
		format("07580008213") should be ("07580 008213")
		format("0758-000-8213") should be ("07580 008213")
		format("00440758-000-8213") should be ("07580 008213")
		format("01530222134") should be ("01530 222134")
		format("  0153   022  21 34") should be ("01530 222134")
		format("+447580008213") should be ("07580 008213")
		format("+44 24 7625 8177") should be ("024 7625 8177")
	}

	@Test def international {
		format("+358456786784") should be ("+358 45 6786784")
		format("+35315324335") should be ("+353 1 532 4335")
		format("+46704523519") should be ("+46 70 452 35 19")
	}

}
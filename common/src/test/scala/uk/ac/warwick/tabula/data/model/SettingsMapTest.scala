package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase

// scalastyle:off magic.number
class SettingsMapTest extends TestBase {

	class TestSettingsMap extends SettingsMap
	trait TestValues { self: SettingsMap =>
		this ++= (
			"string setting" -> "tease",
			"bool setting" -> false,
			"int setting" -> 7
		)
	}

	@Test def emptyDefaults() {
		new TestSettingsMap {
			settingsSeq should be (Seq())
			getSetting("some setting") should be (None)
			getStringSetting("string setting", "default") should be ("default")
			getBooleanSetting("bool setting", true) should be (true)
			getIntSetting("int setting", 5) should be (5)
		}
	}

	@Test def valuesFound() {
		new TestSettingsMap with TestValues {
			getSetting("string setting") should be (Some("tease"))
			getStringSetting("string setting", "default") should be ("tease")
			getBooleanSetting("bool setting", true) should be (false)
			getIntSetting("int setting", 5) should be (7)
			settingsSeq should be (Seq(
					("string setting" -> "tease"),
					("bool setting" -> false),
					("int setting" -> 7)))
		}
	}

	@Test def incompatibleType() {
		new TestSettingsMap with TestValues {
			getBooleanSetting("string setting", false) should be (false)
			getStringSetting("bool setting", "default") should be ("default")
			getIntSetting("string setting", 9) should be (9)
		}
	}

}
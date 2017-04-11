package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase

class NestedSettingsTest extends TestBase {

	class TestSettingsMap extends SettingsMap
	trait TestValues { self: SettingsMap =>
		this ++= (
			"string setting" -> "tease",
			"bool setting" -> false,
			"int setting" -> 7,
			"int.nested" -> 10
		)
	}

	@Test def itWorks {
		new TestSettingsMap with TestValues {
			val nested: NestedSettings = nestedSettings("int")
			nested.IntSetting("nested", 5).value should be (10)
			nested.IntSetting("other", 5).value should be (5)
			nested.toStringProps should be (Seq("nested" -> 10))

			nested.IntSetting("other", 188).value = 5
			nested.toStringProps.toMap should be (Seq("nested" -> 10, "other" -> 5).toMap)
		}
	}

}

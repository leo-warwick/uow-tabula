package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import collection.JavaConversions._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering

// scalastyle:off magic.number
class DateBuilderTest extends TestBase with FreemarkerRendering {

	import DateBuilder._

	@Test def formatting {
		withFakeTime(new DateTime(2012,4,12, 13,36,0)) {

			val anotherDay = new DateTime(2012,3,10, 12,13,14)

			val yesterday = new DateTime().minusDays(1)
			val tomorrow = new DateTime().plusDays(1)
			val today = new DateTime()

			// Test default settings
			format(anotherDay) should be ("12:13&#8194;Sat 10<sup>th</sup> March 2012")

			format(anotherDay, false, false, false, true, true, false, false, true) should be ("12:13&#8194;Sat 10<sup>th</sup> March 2012")
			format(anotherDay, true, true, true, true, true, false, false, true) should be ("12:13:14 (GMT)&#8194;Sat 10<sup>th</sup> March 2012")

			format(yesterday, false, true, false, true, true, false, false, true) should be ("13:36&#8194;Yesterday")
			format(tomorrow, true, true, false, false, true, false, false, true) should be ("13:36:00&#8194;tomorrow")
			format(today, false, false, false, true, true, false, false, true) should be ("13:36&#8194;Today")

			format(today, false, false, false, true, false, false, false, true) should be ("13:36&#8194;Thu 12<sup>th</sup> April 2012")

			// Freemarker exec
			implicit val fmConfig = newFreemarkerConfiguration
			val rendered = renderToString("dateBuilder.ftl", Map("b" -> new DateBuilder, "today" -> today))
			rendered should be ("13:36&#8194;Today")
		}
	}

	@Test def ordinals {
		for ((i, o) <- Seq(
				0->"th",
				1->"st",
				2->"nd",
				3->"rd",
				4->"th",
				5->"th",
				10->"th",
				11->"th",
				12->"th",
				13->"th",
				20->"th",
				21->"st",
				101->"st"
		)) withClue("Ordinal of "+i) { ordinal(i) should be (o) }
	}
}
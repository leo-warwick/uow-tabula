package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import collection.JavaConversions._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import org.joda.time.LocalTime

// scalastyle:off magic.number
class TimeBuilderTest extends TestBase with FreemarkerRendering {

	import TimeBuilder._

	@Test def formatting {
		withFakeTime(new DateTime(2012,4,12, 13,36,44)) {
			val time = new LocalTime()
			val elevenPm = new LocalTime(23, 35, 19)
			val justPastMidnight = new LocalTime(0, 0, 1)

			// Test default settings
			format(time) should be ("13:36")

			format(time, false, false) should be ("1:36pm")
			format(time, false, true) should be ("1:36:44pm")
			format(time, true, false) should be ("13:36")
			format(time, true, true) should be ("13:36:44")

			// Test that with 24-hour=false, we still return as expected for double-digit hours and the annoying midnight case
			format(elevenPm, false, false) should be ("11:35pm")
			format(justPastMidnight, false, false) should be ("12:00am")
			format(justPastMidnight, false, true) should be ("12:00:01am")

			// Freemarker exec
			implicit val fmConfig = newFreemarkerConfiguration
			val rendered = renderToString("timeBuilder.ftl", Map("b" -> new TimeBuilder, "now" -> time))
			rendered should be ("1:36pm")
		}
	}
}
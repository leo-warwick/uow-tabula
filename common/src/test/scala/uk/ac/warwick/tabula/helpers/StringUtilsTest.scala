package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.{Fixtures, TestBase}
import org.junit.Test

import scala.util.Random

class StringUtilsTest extends TestBase with StringUtils {

	@Test def superString {
		val empty:String = ""
		val whitespace:String = "  "
		val nullString:String = null

		empty.hasText should be (false)
		empty.hasLength should be (false)

		whitespace.hasText should be (false)
		whitespace.hasLength should be (true)

		// should even work on null values! as long as the variable is String type
		nullString.hasText should be (false)
		nullString.hasText should be (false)

		"bo".orEmpty should be ("bo")
		empty.orEmpty should be ("")
		whitespace.orEmpty should be ("  ")
		nullString.orEmpty should be ("")

		"bo".maybeText should be (Some("bo"))
		empty.maybeText should be (None)
		whitespace.maybeText should be (None)
		nullString.maybeText should be (None)

		"bo".textOrEmpty should be ("bo")
		empty.textOrEmpty should be ("")
		whitespace.textOrEmpty should be ("")
		nullString.textOrEmpty should be ("")
	}

	@Test def compareAlphaNumeric {
		val s1 = "Group 1"
		val s2 = "group 2"
		val s3 = "group 20"
		val s4 = "Group 10"
		val s5 = "Group 9"
		val s6 = "Late group 1"
		val s7 = "1"
		val s8 = "10"
		val s9 = "2"

		for (i <- 1 to 10) {
			val shuffled = Random.shuffle(Seq(s1, s2, s3, s4, s5, s6, s7, s8, s9))

			shuffled.sorted(StringUtils.AlphaNumericStringOrdering) should be (Seq(s7, s9, s8, s1, s2, s5, s4, s3, s6))
		}
	}

}
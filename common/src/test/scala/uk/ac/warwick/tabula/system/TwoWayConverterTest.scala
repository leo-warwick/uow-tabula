package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import org.joda.time.Days
import org.springframework.core.convert.TypeDescriptor
import java.util.Locale
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.data.convert.MeetingFormatConverter
import uk.ac.warwick.tabula.data.model.MeetingFormat

class TwoWayConverterTest extends TestBase {

	@Test def converting {
		val converter = new DaysConverter
		converter.convert("3", descriptor[String], descriptor[Days]) should be (Days.THREE)
		converter.convert(Days.FIVE, descriptor[Days], descriptor[String]) should be ("5")
	}

	@Test def formatting {
		val converter = new DaysConverter
		converter.parse("3", Locale.getDefault()) should be (Days.THREE)
		converter.print(Days.FIVE, Locale.getDefault()) should be ("5")
	}

	/** Not specifically testing case objects, but generally any object that
		* is a subclass of the converter's declared class. Checking that the
		* internal matching() method is doing the right thing.
		*/
	@Test def caseObjects {
		val converter = new MeetingFormatConverter
		val sourceValue = MeetingFormat.FaceToFace
		val sourceType = TypeDescriptor.valueOf(MeetingFormat.FaceToFace.getClass)
		val targetType = TypeDescriptor.valueOf(classOf[String])
		val faceToFace = converter.convert(sourceValue, sourceType, targetType)
		faceToFace should be ("f2f")
	}

	private def descriptor[A](implicit tag: ClassTag[A]) = TypeDescriptor.valueOf(tag.runtimeClass)
}



/*
 * Test converter which converts from a string of a number into a Days instance,
 * or null if invalid string.
 *
 * "4" -> Days.FOUR
 * Days.NINE -> "9"
 *
 * etc.
 */
class DaysConverter extends TwoWayConverter[String, Days] {
	def convertRight(source: String): Days =
		try {
			Days.days(source.toInt)
		} catch {
			case _:NumberFormatException => null
		}

	def convertLeft(source: Days): String = source.getDays.toString
}

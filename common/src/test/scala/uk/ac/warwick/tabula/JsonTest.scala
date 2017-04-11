package uk.ac.warwick.tabula
import scala.collection.mutable.{Map => MutableMap}
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.StringReader
import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.{DateTimeZone, Chronology, LocalDate, DateTime}
import scala.beans.BeanProperty

class JsonTest extends TestBase {

	val m: ObjectMapper = new JsonObjectMapperFactory().createInstance()

	val now: DateTime = new DateTime().withZone(DateTimeZone.UTC).withDate(2013,7,29).withTime(12,0,0,0)

	// Test DefaultScalaModule from https://github.com/FasterXML/jackson-module-scala
	// which teaches Jackson about Scala collections.
	@Test def encodeMap {
		m.writeValueAsString(Map()) should be("{}")

		m.writeValueAsString(Map("animals" -> Array("cat","dog"))) should be("""{"animals":["cat","dog"]}""")
		m.writeValueAsString(Map("animals" -> JArrayList("cat","dog"))) should be("""{"animals":["cat","dog"]}""")

		m.writeValueAsString(Map("animals" -> List("cat","dog"))) should be("""{"animals":["cat","dog"]}""")
	}

	@Test def parseArray() {
		val map = m.readValue("""{"numbers":[1,2,3]}""", classOf[Map[String, Any]])
		map should be (Map("numbers" -> Seq(1,2,3)))
		// test pattern matching - arrays match Seq[_]
		map("numbers") match {
			case s: Seq[_] => s should be (Seq(1,2,3))
		}
	}

	@Test def parseNumbers {
		val props = """{"age" : 23, "filetypes":["pdf","doc","docx"]}"""
		val map = m.readValue(new StringReader(props), classOf[Map[String,Any]])
		map("filetypes").asInstanceOf[Seq[_]] should equal (Seq("pdf","doc","docx"))
		map("age").asInstanceOf[Int] should be (23)
	}

	class DateHolder(time: DateTime) {
		@BeanProperty var beanPropDate: DateTime = time
		var regularDate: DateTime = time
	}

	@Test def datesBecomeTimestamps() {
		m.writeValueAsString(now) should be ("1375099200000")
		m.writeValueAsString(now.toDate) should be ("1375099200000")
		m.readValue("1375099200000", classOf[DateTime]) should be (now)
	}

	// Local date format by default is array of nums [y,m,d]
	@Test def localDatesBecomeIntArrays() {
		m.writeValueAsString(now.toLocalDate) should be ("[2013,7,29]")
		m.readValue("[2013,7,29]", classOf[LocalDate])
	}

	// was a bug in scala jackson module
	@Test def mutableMapWorks {
		m.writeValueAsString(MutableMap()) should be("{}")
	}
}
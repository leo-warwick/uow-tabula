package uk.ac.warwick.tabula.web.views
import java.io.StringWriter

import freemarker.template.Configuration
import org.joda.time.Duration
import org.junit.Before
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Routes

import scala.util.Properties

class FreemarkerEngineTest extends TestBase {
	var configuration:Configuration = _

	@Before def setup() {
		configuration = newFreemarkerConfiguration()
		Routes.coursework._cm1Prefix = Some("coursework")
	}

	def render(template:String, map:Map[String,Any]): String = {
		val writer = new StringWriter
		configuration.getTemplate(template).process(map, writer)
		writer.toString
	}

	@Test def plain() {
		val num:Option[Duration] = Some(Duration.standardSeconds(1))
		val output = render("plain.ftl", Map(
				"specifiedValue" -> "Specified value.",
				"longnum" -> num
		))
		output should be ("No frills template #1. Unspecified value. Specified value.")
	}

	/**
	 * Accessing the Routes object from FTL now works as this test demonstrates,
	 * but at the time of writing we don't use it. It could replace the routes.ftl
	 */
	@Test def routes() {
		val department = new Department
		department.code = "la"
		val output = render("renderroutes.ftl", Map("Routes"->Routes, "department" -> department))
		output should be ("The path to department LA is /coursework/admin/department/la/")
	}

	@Test def someAndNon() {
		val output = render("somenone.ftl", Map(
			"some" -> Some("CERBERUS"),
			"none" -> None
		))
		output should be ("Robot=CERBERUS;")
	}

  @Test def usesMacros() {
    val output=render("uses_macros.ftl",Map("count"->3))
    output should be ("First line"+Properties.lineSeparator+"3 wombats")
  }

}
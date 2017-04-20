package uk.ac.warwick.tabula.web.views

import java.io.{StringReader, StringWriter}
import java.util.Properties

import freemarker.core.Environment
import freemarker.template._
import org.junit.Before
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{Mockito, TestBase}

class UrlMethodModelTest extends TestBase with Mockito {

	val model: UrlMethodModel = new UrlMethodModel

	@Before def setup() {
		model.context = "/courses"
		model.toplevelUrl = "https://courses.warwick.ac.uk"
		model.staticHashes = new Properties
	}

	@Test def fn() {
		model.exec(JArrayList("/module/yes", "/")).toString should be ("/module/yes")
		model.exec(JArrayList("/module/yes")).toString should be ("/courses/module/yes")
		model.exec(JArrayList("/module/yes", "/profiles")).toString should be ("/profiles/module/yes")
	}

	@Test def encoding() {
		val input = "/download/greek \u03a7\u03a8\u03a9.doc"
		val expected = "/courses/download/greek%20%CE%A7%CE%A8%CE%A9.doc"
		model.exec(JArrayList(input)).toString should be (expected)
	}

	@Test def tagPageAndContext() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		hash.put("page", "/module/yes")
		hash.put("context", "/")

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), hash, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("page", hash.get("page"))
		params.put("context", hash.get("context"))

		model.execute(env, params, null, body)

		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/module/yes")
	}

	@Test def tagPageNoContext() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		hash.put("page", "/module/yes")

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), hash, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("page", hash.get("page"))

		model.execute(env, params, null, body)

		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/courses/module/yes")
	}

	@Test def tagResource() {
		// Use a SimpleHash as a workaround to wrapping things manually
		val hash = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		hash.put("resource", "/static/css/main.css")

		model.staticHashes.setProperty("css/main.css", "1234567890")

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), hash, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("resource", hash.get("resource"))

		model.execute(env, params, null, body)

		writer.getBuffer().toString() should be ("https://courses.warwick.ac.uk/static/css/main.css.1234567890")
	}

}
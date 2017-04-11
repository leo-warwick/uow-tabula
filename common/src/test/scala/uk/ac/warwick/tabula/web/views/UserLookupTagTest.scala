package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.Fixtures
import freemarker.core.Environment
import java.io.StringWriter
import java.io.StringReader
import uk.ac.warwick.tabula.MockUserLookup
import freemarker.template.utility.DeepUnwrap
import scala.collection.JavaConverters._

class UserLookupTagTest extends TestBase with Mockito {

	val tag = new UserLookupTag

	val userLookup = new MockUserLookup
	tag.userLookup = userLookup

	userLookup.registerUsers("cuscav", "cusebr")
	userLookup.getUserByUserId("cuscav").setWarwickId("0672089")
	userLookup.getUserByUserId("cusebr").setWarwickId("0672088")

	@Test def singleId = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("id", "cuscav")

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("id", model.get("id"))

		tag.execute(env, params, null, body)

		verify(body, times(1)).render(writer)

		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_user")) should be (userLookup.getUserByUserId("cuscav"))
	}

	@Test def multipleIds = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val ids: JList[String] = JArrayList()
		ids.add("cuscav")
		ids.add("cusebr")

		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("ids", ids)

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("ids", model.get("ids"))

		tag.execute(env, params, null, body)

		verify(body, times(1)).render(writer)

		DeepUnwrap.unwrap(env.getCurrentNamespace().get("missing_ids")) should be (Seq())
		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_users")) should be (Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
	}

	@Test def singleUniversityId = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("id", "0672089")
		model.put("lookupByUniversityId", true)

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("id", model.get("id"))
		params.put("lookupByUniversityId", model.get("lookupByUniversityId"))

		tag.execute(env, params, null, body)

		verify(body, times(1)).render(writer)

		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_user")) should be (userLookup.getUserByUserId("cuscav"))
	}

	@Test def multipleUniversityIds = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val ids: JList[String] = JArrayList()
		ids.add("0672089")
		ids.add("0672088")

		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("ids", ids)
		model.put("lookupByUniversityId", true)

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("ids", model.get("ids"))
		params.put("lookupByUniversityId", model.get("lookupByUniversityId"))

		tag.execute(env, params, null, body)

		verify(body, times(1)).render(writer)

		DeepUnwrap.unwrap(env.getCurrentNamespace().get("missing_ids")) should be (Seq())
		DeepUnwrap.unwrap(env.getCurrentNamespace().get("returned_users")) should be (Map(
			"0672089" -> userLookup.getUserByUserId("cuscav"),
			"0672088" -> userLookup.getUserByUserId("cusebr")
		))
	}

}
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

class PermissionTagTest extends TestBase with Mockito {

	val tag = new PermissionTag

	val securityService: SecurityService = mock[SecurityService]
	tag.securityService = securityService

	@Test def can = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("permissionName", "Module.Create")
		model.put("scope", dept)

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("action", model.get("permissionName"))
		params.put("object", model.get("scope"))

		securityService.can(currentUser, Permissions.Module.Create, dept) returns (true)

		tag.execute(env, params, null, body)

		verify(body, times(1)).render(writer)
	}

	@Test def cannot = withUser("cuscav") {
		val dept = Fixtures.department("in")

		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("permissionName", "Module.Create")
		model.put("scope", dept)

		val writer = new StringWriter

		val env = new Environment(new Template("temp", new StringReader(""), null), model, writer)
		val body = mock[TemplateDirectiveBody]

		val params = new java.util.HashMap[String, TemplateModel]
		params.put("action", model.get("permissionName"))
		params.put("object", model.get("scope"))

		securityService.can(currentUser, Permissions.Module.Create, dept) returns (false)

		tag.execute(env, params, null, body)

		verify(body, times(0)).render(writer)
	}
}
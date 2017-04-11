package uk.ac.warwick.tabula.web.controllers.ajax

import java.io.StringWriter

import uk.ac.warwick.tabula.{JsonObjectMapperFactory, MockUserLookup, TestBase}

class UserPickerControllerTest extends TestBase {

	val controller = new UserPickerController
	controller.json = new JsonObjectMapperFactory().createInstance

	@Test def queryJson {
		val cmd = new UserPickerController.UserPickerCommand()

		val userLookup = new MockUserLookup
		userLookup.registerUsers("cuscav", "cusebr")

		userLookup.addFindUsersWithFilterResult(userLookup.getUserByUserId("cuscav"))
		userLookup.addFindUsersWithFilterResult(userLookup.getUserByUserId("cusebr"))
		userLookup.findUsersEnabled = true

		cmd.userLookup = userLookup

		val writer = new StringWriter
		controller.queryJson(cmd, writer)

		val users = controller.json.readValue(writer.getBuffer().toString(), classOf[Seq[scala.collection.mutable.Map[String, Any]]])
		users.length should be (2)

		// Ensure order has been retained
		users(0).get("value") should be (Some("cuscav"))
		users(1).get("value") should be (Some("cusebr"))
	}

	@Test def setQuery {
		val cmd = new UserPickerController.UserPickerCommand()

		cmd.query = "billy"
		cmd.query should be ("billy")
		cmd.firstName should be ("")
		cmd.lastName should be ("billy")

		cmd.query = "billy bob"
		cmd.query should be ("billy bob")
		cmd.firstName should be ("billy")
		cmd.lastName should be ("bob")

		// TODO is this actually the right behaviour?
		cmd.query = "billy bob thornton"
		cmd.query should be ("billy bob")
		cmd.firstName should be ("billy")
		cmd.lastName should be ("bob")
	}

}
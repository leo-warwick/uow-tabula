package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionChangedNotificatonTest extends TestBase with Mockito with ExtensionNotificationTesting {

	val TEST_CONTENT = "test"

	def createNotification(extension: Extension, student: User, actor: User): ExtensionChangedNotification = {
		val n = Notification.init(new ExtensionChangedNotification, actor, Seq(extension), extension.assignment)
		wireUserlookup(n, student)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n: ExtensionChangedNotification = createNotification(extension, student, admin)
		n.url should be(s"/$cm1Prefix/module/xxx/123/")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new ExtensionFixture{
		val n: ExtensionChangedNotification = createNotification(extension, student, admin)
		n.recipients should be (Seq(student))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n: ExtensionChangedNotification = createNotification(extension, student, admin)
		n.content.template should be ("/WEB-INF/freemarker/emails/modified_manual_extension.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n: ExtensionChangedNotification = createNotification(extension, student, admin)

		n.content.model.get("extension").get should be(extension)
		n.content.model.get("newExpiryDate").get should be("23 August 2013 at 12:00:00")
		n.content.model.get("assignment").get should be(assignment)
		n.content.model.get("module").get should be(module)
		n.content.model.get("user").get should be(student)
		n.content.model.get("path").get should be(s"/$cm1Prefix/module/xxx/123/")
	}

	@Test
	def title() { new ExtensionFixture {
		module.code = "cs118"
		assignment.name = "5,000 word essay"
		student.setFullName("John Studentson")

		val n: ExtensionChangedNotification = createNotification(extension, student, admin)
		n.title should be ("CS118: Your extended deadline for \"5,000 word essay\" has changed")
	}}

}

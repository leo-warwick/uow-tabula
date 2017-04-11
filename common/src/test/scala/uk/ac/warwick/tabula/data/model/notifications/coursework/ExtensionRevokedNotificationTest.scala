package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionRevokedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {


	def createNotification(assignment: Assignment, student: User, actor: User): ExtensionRevokedNotification = {
		val n = Notification.init(new ExtensionRevokedNotification, actor, Seq(assignment))
		n.recipientUniversityId = student.getUserId
		wireUserlookup(n, student)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n: ExtensionRevokedNotification = createNotification(assignment, student, admin)
		n.url should be(s"/$cm1Prefix/module/xxx/123/")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new ExtensionFixture{
		val n: ExtensionRevokedNotification = createNotification(assignment, student, admin)
		n.recipients should be (Seq(student))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n: ExtensionRevokedNotification = createNotification(assignment, student, admin)
		n.content.template should be ("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n: ExtensionRevokedNotification = createNotification(assignment, student, admin)
		n.content.model("originalAssignmentDate") should be("1 August 2013 at 12:00:00")
		n.content.model("assignment") should be(assignment)
		n.content.model("module") should be(module)
		n.content.model("user") should be(student)
		n.url should be(s"/$cm1Prefix/module/xxx/123/")
	}

	@Test
	def title() { new ExtensionFixture {
		module.code = "cs118"
		assignment.name = "5,000 word essay"
		student.setFullName("John Studentson")

		val n: ExtensionRevokedNotification = createNotification(assignment, student, admin)
		n.title should be ("CS118: Your extended deadline for \"5,000 word essay\" has been revoked")
	}}
}

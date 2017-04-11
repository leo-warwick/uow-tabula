package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionRequestRespondedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {

	def createNotification(extension: Extension, student: User, actor: User): ExtensionRequestRespondedNotification = {
		val baseNotification = if (extension.approved) {
			new ExtensionRequestRespondedApproveNotification
		} else {
			new ExtensionRequestRespondedRejectNotification
		}
		val n = Notification.init(baseNotification, actor, Seq(extension), extension.assignment)
		wireUserlookup(n, student)
		n
	}


	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.url should be(s"/$cm1Prefix/admin/module/xxx/assignments/123/extensions?usercode=student")
	}

	@Test
	def titleShouldContainMessage():Unit = new ExtensionFixture {
		extension.reject()
		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.title.contains("XXX: Extension request by [Unknown user] for \"Essay\" was rejected") should be {true}
	}

	@Test
	def recipientsContainsOtherAdmins():Unit = new ExtensionFixture{
		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.recipients should be (Seq(admin2, admin3))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.content.template should be ("/WEB-INF/freemarker/emails/responded_extension_request.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		extension.reject()
		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.content.model.get("studentName").get should be("[Unknown user]")
		n.content.model.get("agentName").get should be("[Unknown user]")
		n.content.model.get("assignment").get should be(assignment)
		n.content.model.get("verbed").get should be("rejected")
		n.content.model.get("path").get should be(s"/$cm1Prefix/admin/module/xxx/assignments/123/extensions?usercode=student")
	}

	@Test
	def titleApproved() { new ExtensionFixture {
		module.code = "cs118"
		assignment.name = "5,000 word essay"
		student.setFullName("John Studentson")
		extension.approve()

		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.title should be ("CS118: Extension request by John Studentson for \"5,000 word essay\" was approved")
	}}

	@Test
	def titleRejected() { new ExtensionFixture {
		module.code = "cs118"
		assignment.name = "5,000 word essay"
		student.setFullName("John Studentson")
		extension.reject()

		val n: ExtensionRequestRespondedNotification = createNotification(extension, student, admin)
		n.title should be ("CS118: Extension request by John Studentson for \"5,000 word essay\" was rejected")
	}}
 }

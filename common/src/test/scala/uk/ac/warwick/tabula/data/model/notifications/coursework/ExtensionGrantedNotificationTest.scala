package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionGrantedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {

  def createNotification(extension: Extension, student: User, actor: User): ExtensionGrantedNotification = {
    val n = Notification.init(new ExtensionGrantedNotification, actor, Seq(extension), extension.assignment)
    wireUserlookup(n, student)
    n
  }

  @Test
  def urlIsSubmissionPage(): Unit = new ExtensionFixture {
    val n: ExtensionGrantedNotification = createNotification(extension, student, admin)
    n.url should be("/coursework/submission/123/")
  }

  @Test
  def recipientsContainsSingleUser(): Unit = new ExtensionFixture {
    val n: ExtensionGrantedNotification = createNotification(extension, student, admin)
    n.recipients should be(Seq(student))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = new ExtensionFixture {
    val n: ExtensionGrantedNotification = createNotification(extension, student, admin)
    n.content.template should be("/WEB-INF/freemarker/emails/new_manual_extension.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    val n: ExtensionGrantedNotification = createNotification(extension, student, admin)
    n.content.model.get("extension").get should be(extension)
    n.content.model.get("newExpiryDate").get should be("23 August 2013 at 12:00:00")
    n.content.model.get("assignment").get should be(assignment)
    n.content.model.get("module").get should be(module)
    n.content.model.get("user").get should be(student)
  }

  @Test
  def title(): Unit = {
    new ExtensionFixture {
      module.code = "cs118"
      assignment.name = "5,000 word essay"
      student.setFullName("John Studentson")

      val n: ExtensionGrantedNotification = createNotification(extension, student, admin)
      n.title should be("CS118: Your deadline for \"5,000 word essay\" has been extended")
    }
  }
}

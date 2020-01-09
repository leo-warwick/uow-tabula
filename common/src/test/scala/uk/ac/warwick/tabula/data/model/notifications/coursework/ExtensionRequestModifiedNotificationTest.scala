package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ExtensionRequestModifiedNotificationTest extends TestBase with ExtensionNotificationTesting with Mockito {

  def createNotification(extension: Extension, student: User): ExtensionRequestModifiedNotification = {
    val n = Notification.init(new ExtensionRequestModifiedNotification, student, Seq(extension), extension.assignment)
    n.userLookup = mockUserLookup
    n.profileService = mockProfileService
    n.relationshipService = mockRelationshipService

    wireUserlookup(n, student)
    n.profileService.getMemberByUniversityId(student.getWarwickId) returns None

    n
  }

  @Test
  def urlIsAssignmentExtensionsPage(): Unit = new ExtensionFixture {
    val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
    n.url should be(s"/coursework/admin/assignments/123/extensions?extension=${extension.id}")
  }

  @Test
  def titleShouldContainMessage(): Unit = new ExtensionFixture {
    val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
    n.title.contains("Extension request modified") should be(true)
  }


  @Test
  def recipientsContainsAllAdmins(): Unit = new ExtensionFixture {
    val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
    n.recipients should be(Seq(admin, admin2, admin3))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = new ExtensionFixture {
    val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
    n.content.template should be("/WEB-INF/freemarker/emails/modified_extension_request.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
    n.content.model("requestedExpiryDate") should be("23 August 2013 at 12:00:00")
    n.content.model.get("reasonForRequest") should be(Symbol("empty"))
    n.url should be(s"/coursework/admin/assignments/123/extensions?extension=${extension.id}")
    n.content.model("assignment") should be(assignment)
    n.content.model("student") should be(student)
  }

  @Test
  def title(): Unit = {
    new ExtensionFixture {
      module.code = "cs118"
      assignment.name = "5,000 word essay"
      student.setFullName("John Studentson")

      val n: ExtensionRequestModifiedNotification = createNotification(extension, student)
      n.title should be("CS118: Extension request modified by John Studentson for \"5,000 word essay\"")
    }
  }

}

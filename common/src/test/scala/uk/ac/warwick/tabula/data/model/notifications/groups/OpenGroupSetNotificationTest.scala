package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.{Mockito, SmallGroupFixture, TestBase}
import uk.ac.warwick.userlookup.User

class OpenGroupSetNotificationTest extends TestBase with Mockito {

	def createNotification(groups: Seq[SmallGroupSet], actor: User, recipient: User): OpenSmallGroupSetsStudentSignUpNotification = {
		val n = Notification.init(new OpenSmallGroupSetsStudentSignUpNotification, actor, groups)
		n.recipientUserId = recipient.getUserId
		n
	}

	@Test
	def titleIncludesGroupFormats() { new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification =  createNotification(Seq(groupSet1, groupSet2, groupSet3), actor, recipient)
		n.userLookup = userLookup
		n.title should be("LA101, LA102 and LA103 labs, seminars and tutorials are now open for sign up")
	}}

	@Test
	def titleMakesSenseWithASingleGroup() { new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification = createNotification(Seq(groupSet1),actor, recipient)
		n.userLookup = userLookup
		n.title should be ("LA101 labs are now open for sign up")
	}}

	@Test
	def urlIsSmallGroupTeachingHomePage(): Unit = new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification =  createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.url should be("/groups")
	}

	@Test
	def recipientsContainsSingleUser(): Unit  = new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification = createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.recipients should be (Seq(recipient))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate(): Unit = new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification = createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.content.template should be ("/WEB-INF/freemarker/notifications/groups/open_small_group_student_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel(): Unit = new SmallGroupFixture {
		val n: OpenSmallGroupSetsStudentSignUpNotification = createNotification(Seq(groupSet1), actor, recipient)
		val content: FreemarkerModel = n.content
		content.model("profileUrl") should be("/groups")
		content.model("groupsets") should be(Seq(groupSet1))
	}

}

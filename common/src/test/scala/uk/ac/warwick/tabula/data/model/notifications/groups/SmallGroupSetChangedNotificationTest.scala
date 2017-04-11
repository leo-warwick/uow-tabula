package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.{Module, Notification}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.{Fixtures, Mockito, SmallGroupFixture, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class SmallGroupSetChangedNotificationTest extends TestBase with Mockito {

	val module: Module = Fixtures.module("cs118")

	val set: SmallGroupSet = Fixtures.smallGroupSet("set 1")
	set.format = SmallGroupFormat.Seminar
	set.module = module

	val group: SmallGroup = Fixtures.smallGroup("group 1")
	group.groupSet = set

	@Test def titleStudent() = withUser("cuscav", "0672089") {
		 val notification = Notification.init(new SmallGroupSetChangedStudentNotification, currentUser.apparentUser, group, set)
		 notification.title should be ("CS118: Your seminar allocation has changed")
	}

	@Test def titleTutor() = withUser("cuscav", "0672089") {
		val notification = Notification.init(new SmallGroupSetChangedTutorNotification, currentUser.apparentUser, group, set)
		notification.title should be ("CS118: Your seminar allocation has changed")
	}

	def createStudentNotification(groupSet:SmallGroupSet, actor:User, recipient:User): SmallGroupSetChangedStudentNotification = {
		val n = Notification.init(new SmallGroupSetChangedStudentNotification, actor, groupSet.groups.asScala, groupSet)
		n.recipientUserId = recipient.getUserId
		n
	}

	def createTutorNotification(groupSet:SmallGroupSet, actor:User, recipient:User): SmallGroupSetChangedTutorNotification = {
		val n = Notification.init(new SmallGroupSetChangedTutorNotification, actor, groupSet.groups.asScala, groupSet)
		n.recipientUserId = recipient.getUserId
		n
	}

	@Test
	def urlIsProfilePageForStudent():Unit = new SmallGroupFixture{
		val n: SmallGroupSetChangedStudentNotification =  createStudentNotification(groupSet1, actor, recipient)
		n.userLookup = userLookup
		n.url should be(s"/profiles/view/${recipient.getWarwickId}/seminars")
	}


	@Test
	def urlIsGroupsPageForTutor(): Unit = new SmallGroupFixture {
		val n: SmallGroupSetChangedTutorNotification = createTutorNotification(groupSet1, actor, recipient)
		n.url should be(Routes.tutor.mygroups)
	}

	@Test
	def titleIsHardcoded(){new SmallGroupFixture {
		val n: SmallGroupSetChangedStudentNotification =  createStudentNotification(groupSet1, actor, recipient)
		n.title should be("LA101: Your lab allocation has changed")
	}}

	@Test
	def shouldCallTextRendererWithCorrectTemplateAndModel():Unit = new SmallGroupFixture {
		val n: SmallGroupSetChangedStudentNotification = createStudentNotification(groupSet1, actor, recipient)
		n.userLookup = userLookup
		n.content.template should be (SmallGroupSetChangedNotification.templateLocation)
		n.content.model.get("profileUrl") should be(Some(s"/profiles/view/${recipient.getWarwickId}/seminars"))
		n.content.model.get("groupSet") should be(Some(groupSet1))
	}

}

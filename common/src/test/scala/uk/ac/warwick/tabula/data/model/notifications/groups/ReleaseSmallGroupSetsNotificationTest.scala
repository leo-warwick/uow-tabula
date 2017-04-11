package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.{Module, Notification}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.{Fixtures, Mockito, SmallGroupFixture, TestBase}
import uk.ac.warwick.userlookup.User

class ReleaseSmallGroupSetsNotificationTest extends TestBase with Mockito {

	val module1: Module = Fixtures.module("cs118")
	val module2: Module = Fixtures.module("cs119")
	val module3: Module = Fixtures.module("cs120")

	val set1: SmallGroupSet = Fixtures.smallGroupSet("set 1")
	set1.format = SmallGroupFormat.Seminar
	set1.module = module1

	val set2: SmallGroupSet = Fixtures.smallGroupSet("set 2")
	set2.format = SmallGroupFormat.Seminar
	set2.module = module1

	val set3: SmallGroupSet = Fixtures.smallGroupSet("set 3")
	set3.format = SmallGroupFormat.Seminar
	set3.module = module1

	val group1: SmallGroup = Fixtures.smallGroup("group 1")
	group1.groupSet = set1

	val group2: SmallGroup = Fixtures.smallGroup("group 2")
	group2.groupSet = set2

	val group3: SmallGroup = Fixtures.smallGroup("group 3")
	group3.groupSet = set3

	@Test def title() = withUser("cuscav", "0672089") {
		 val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, group1)
		 notification.title should be ("CS118 seminar allocation")
	}

	@Test def titlePlural() = withUser("cuscav", "0672089") {
		val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, Seq(group1, group2, group3))
		notification.title should be ("CS118 seminar allocations")

		set2.module = module3
		set2.format = SmallGroupFormat.Workshop
		notification.title should be ("CS118 and CS120 seminar and workshop allocations")

		set3.module = module2
		set2.format = SmallGroupFormat.Example
		notification.title should be ("CS118, CS119 and CS120 seminar and example class allocations")
	}

	val TEST_CONTENT = "test"
	def createNotification(group:SmallGroup, actor:User,recipient:User, isStudent:Boolean = true): ReleaseSmallGroupSetsNotification =
		createMultiGroupNotification(Seq(group), actor, recipient, isStudent)

	def createMultiGroupNotification(groups:Seq[SmallGroup], actor:User,recipient:User, isStudent:Boolean = true): ReleaseSmallGroupSetsNotification = {
		val n = Notification.init(new ReleaseSmallGroupSetsNotification, actor, groups)
		n.recipientUserId = recipient.getUserId
		n.isStudent = isStudent
		n
	}

	@Test
	def titleIncludesGroupFormat(){new SmallGroupFixture {
		val n: ReleaseSmallGroupSetsNotification =  createNotification(group1, actor, recipient)
		n.title should be("LA101 lab allocation")
	}}

	@Test
	def titleJoinsMultipleGroupSetsNicely(){ new SmallGroupFixture{
		val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Seq(group1,group2, group3),actor, recipient)
		n.title should be ("LA101, LA102 and LA103 lab, seminar and tutorial allocations")
	}}

	@Test
	def titleRemovesDuplicateFormats(){ new SmallGroupFixture{
		val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Seq(group1,group2, group3, group4, group5),actor, recipient)
		n.title should be ("LA101, LA102, LA103, LA104 and LA105 lab, seminar and tutorial allocations")
	}}

	@Test(expected = classOf[IllegalArgumentException])
	def cantCreateANotificationWithNoGroups(){ new SmallGroupFixture{
		val n: ReleaseSmallGroupSetsNotification = createMultiGroupNotification(Nil, actor, recipient)
		n.preSave(true)
	}}

	@Test
	def urlIsProfilePageForStudents():Unit = new SmallGroupFixture{
		val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient, isStudent = true)
		n.userLookup = userLookup
		n.url should be(s"/profiles/view/${recipient.getWarwickId}/seminars")

	}

	@Test
	def urlIsMyGroupsPageForTutors():Unit = new SmallGroupFixture{
		val n: ReleaseSmallGroupSetsNotification =  createNotification(group1, actor, recipient, isStudent = false)
		n.url should be(Routes.tutor.mygroups)
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
		val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient)
		n.userLookup = userLookup
		n.content.template should be ("/WEB-INF/freemarker/notifications/groups/release_small_group_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new SmallGroupFixture {
		val n: ReleaseSmallGroupSetsNotification = createNotification(group1, actor, recipient)
		n.userLookup = userLookup
		n.content.model.get("user") should be(Some(recipient))
		n.content.model.get("profileUrl") should be(Some(s"/profiles/view/${recipient.getWarwickId}/seminars"))
		n.content.model.get("groups") should be(Some(List(group1)))
	}

 }

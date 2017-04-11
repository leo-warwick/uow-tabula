package uk.ac.warwick.tabula.data.model

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.JavaImports.{JArrayList, JHashMap, JList}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.{GroupImpl, User}

import scala.collection.JavaConverters._

class UserGroupTest extends PersistenceTestBase  with Mockito{

	val mockSessionFactory: SessionFactory = smartMock[SessionFactory]
	val mockSession: Session = smartMock[Session]
	mockSessionFactory.getCurrentSession returns mockSession
	mockSessionFactory.openSession() returns mockSession

	@Test def membership() {
		transactional { t =>
			var group = UserGroup.ofUsercodes
			group.sessionFactory = mockSessionFactory
			// users that can't be changed (i.e. as imported from upstream)
			group.staticUserIds = Seq( "exoman", "eggdog" )
			// users added manually
			group.addUserId("superhat")
			group.addUserId("menace")

			session.saveOrUpdate(group)
			session.flush()
			session.clear()

			group = session.get(classOf[UserGroup], group.id).asInstanceOf[UserGroup]

			group.staticUserIds.size should be (2)
			group.staticUserIds should (contain ("exoman") and contain ("eggdog"))

			group.includedUserIds.size should be (2)
			group.includedUserIds should (contain ("superhat") and contain ("menace"))

			group.excludeUserId("eggdog") // poor eggdog.
			group.includesUserId("whoareyou") should be {false}
			group.includesUserId("exoman") should be {true}
			group.includesUserId("eggdog") should be {false}
			group.includesUserId("superhat") should be {true}
			group.includesUserId("menace") should be {true}

			/* check that members works and is consistent.
			 * At time of writing, staticIncludeUsers would get
			 * added to includeUsers each time :|
			 */
			group.members.size should be (3)
			group.members.size should be (3)
			group.members.size should be (3)
		}
	}

	@Test def withWebgroup() {
		val userLookup = new MockUserLookup

		val group = UserGroup.ofUsercodes
		group.sessionFactory = mockSessionFactory
		group.userLookup = userLookup

		group.addUserId("cuscav")
		group.addUserId("curef")
		group.excludeUserId("cusmab") // we don't like Steve
		group.staticUserIds = Seq("sb_systemtest")
		group.baseWebgroup = "in-elab"

		val webgroup = new GroupImpl
		webgroup.setUserCodes(List("cuscav", "cusmab", "cusebr").asJava)

		userLookup.groupService.groupMap += ("in-elab" -> webgroup)

		group.members should be (Seq("sb_systemtest", "cuscav", "curef", "cusebr"))
	}

	@Test def copy() {
		val group = UserGroup.ofUsercodes
		group.sessionFactory = mockSessionFactory
		group.addUserId("cuscav")
		group.addUserId("curef")
		group.excludeUserId("cusmab") // we don't like Steve
		group.staticUserIds = Seq("sb_systemtest")
		group.baseWebgroup = "in-elab"

		val group2 = UserGroup.ofUsercodes
		group2.sessionFactory = mockSessionFactory
		group2.copyFrom(group)

		group.eq(group2) should be {false}

		group2.includedUserIds should be (group.includedUserIds)
		group2.excludedUserIds should be (group.excludedUserIds)
		group2.staticUserIds should be (group.staticUserIds)
		group2.baseWebgroup should be (group.baseWebgroup)
		group2.universityIds should be (group.universityIds)
	}

	@Test(expected=classOf[AssertionError])
	def cannotCopyBetweenDifferentGroupTypes() {
		val group = UserGroup.ofUniversityIds
		val group2 = UserGroup.ofUsercodes
		group2.copyFrom(group)
	}

	@Test
	def canGetUsersWhenHoldingUserIds() {
		val test = new User("test")
		val group = UserGroup.ofUsercodes
		group.addUserId("test")
		group.userLookup = mock[UserLookupService]
		group.userLookup.getUsersByUserIds(JArrayList("test")) returns JHashMap("test" -> test)

		group.users should be(Seq(test))
		verify(group.userLookup, times(1)).getUsersByUserIds(any[JList[String]])
	}

	@Test
	def canGetUsersWhenHoldingWarwickIds() {
		val test = new User("test")
		val group = UserGroup.ofUniversityIds
		group.addUserId("test")
		group.userLookup = mock[UserLookupService]
		group.userLookup.getUsersByWarwickUniIds(Seq("test")) returns Map("test" -> test)

		group.users should be(Seq(test))
		verify(group.userLookup, times(1)).getUsersByWarwickUniIds(any[Seq[String]])
	}

	@Test
	def canGetExcludedUsersWhenHoldingUserIds() {
		val test = new User("test")
		val group = UserGroup.ofUsercodes
		group.excludeUserId("test")
		group.userLookup = mock[UserLookupService]
		group.userLookup.getUsersByUserIds(JArrayList("test")) returns JHashMap("test" -> test)

		group.excludes should be(Seq(test))
		verify(group.userLookup, times(1)).getUsersByUserIds(any[JList[String]])
	}

	@Test
	def canGetExcludedUsersWhenHoldingWarwickIds() {
		val test = new User("test")
		val group = UserGroup.ofUniversityIds
		group.excludeUserId("test")
		group.userLookup = mock[UserLookupService]
		group.userLookup.getUsersByWarwickUniIds(Seq("test")) returns Map("test" -> test)

		group.excludes should be(Seq(test))
		verify(group.userLookup, times(1)).getUsersByWarwickUniIds(any[Seq[String]])
	}
}
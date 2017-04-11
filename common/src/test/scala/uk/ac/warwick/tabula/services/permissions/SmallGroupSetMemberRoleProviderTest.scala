package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.roles.SmallGroupSetMember
import uk.ac.warwick.tabula.services.{UserGroupCacheManager, UserLookupService, AssessmentMembershipService}
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.{UnspecifiedTypeUserGroup, UserGroup}

class SmallGroupSetMemberRoleProviderTest extends TestBase with Mockito {

	private trait Fixture{
		val userLookup: UserLookupService = mock[UserLookupService]
		val groupSet = new SmallGroupSet
		groupSet.module = Fixtures.module("in101")
		groupSet.module.adminDepartment = Fixtures.department("in")

		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		wireUserLookup(groupSet.members)
		groupSet.id= "test"

		val membershipService: AssessmentMembershipService = mock[AssessmentMembershipService]
		groupSet.membershipService = membershipService

		val memberUser = new User("member")
		memberUser.setWarwickId("test")
		userLookup.getUserByWarwickUniId("test") returns memberUser

		val member = new CurrentUser(memberUser, memberUser)
		groupSet.members.add(memberUser)
		groupSet.releasedToStudents = true

		val nonMemberUser = new User("nonMember")
		nonMemberUser.setWarwickId("test2")
		val nonMember = new CurrentUser(nonMemberUser, nonMemberUser)
		userLookup.getUserByWarwickUniId("test2") returns nonMemberUser

		membershipService.isStudentMember(memberUser, Nil, Some(groupSet.members)) returns (true)
		membershipService.isStudentMember(nonMemberUser, Nil, Some(groupSet.members)) returns (false)

		val roleProvider = new SmallGroupSetMemberRoleProvider
	}

	@Test
	def membersOfAGroupsetGetTheGroupsetMemberRole(){new Fixture {
		roleProvider.getRolesFor(member,groupSet) should be(Seq(SmallGroupSetMember(groupSet)))
	}}

	@Test
	def nonMembersDontGetTheGroupsetMemberRole(){new Fixture {
		roleProvider.getRolesFor(nonMember,groupSet) should be(Stream.Empty)
	}}


}

package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Manual, StudentSignUp}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.views.GroupsDisplayHelper
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.{Mockito, SmallGroupBuilder, SmallGroupSetBuilder, TestBase}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.JavaConverters._

class HomeControllerTest extends TestBase with Mockito{

	import GroupsDisplayHelper._

	private trait Fixture{
		val userLookup: UserLookupService = mock[UserLookupService]

		val unallocatedUser = new User
		unallocatedUser.setWarwickId("unallocated")
		unallocatedUser.setUserId("unallocated")

		val allocatedUser = new User
		allocatedUser.setWarwickId("allocated")
		allocatedUser.setUserId("allocated")

		val userDatabase = Seq(unallocatedUser, allocatedUser)
		userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { _ match { case ids: Seq[String @unchecked] =>
			ids.map(id => (id, userDatabase.find {_.getWarwickId == id}.getOrElse (new AnonymousUser()))).toMap
		}}

		val group1: SmallGroup = new SmallGroupBuilder()
			.withStudents(createUserGroup(Seq(allocatedUser)))
			.build

		val group2: SmallGroup = new SmallGroupBuilder().build
		val group3: SmallGroup = new SmallGroupBuilder().build

		val groupSet: SmallGroupSet = new SmallGroupSetBuilder()
			.withAllocationMethod(StudentSignUp)
			.withMembers(createUserGroup(Seq(allocatedUser,unallocatedUser)))
			.withGroups(Seq(group1, group2))
			.build
		val groupSet2: SmallGroupSet = new SmallGroupSetBuilder()
			.withAllocationMethod(StudentSignUp)
			.withMembers(createUserGroup(Seq(allocatedUser,unallocatedUser)))
			.withGroups(Seq(group3))
			.withModule(groupSet.module)
			.build

		def createUserGroup(users:Seq[User]): UserGroup = {
			val ug = UserGroup.ofUniversityIds
			ug.userLookup = userLookup
			users.foreach (ug.add)
			ug
		}

	}

	@Test
	def groupsToDisplayReturnsNilForNonSelfSignUpWithNoAllocations() { new Fixture{
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def groupsToDisplayReturnsAllocatedGroupsForNonSelfSignUpWithNoAllocations() { new Fixture{
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def groupstoDisplayReturnsAllGroupsForOpenSelfSignupWithNoAllocation(){ new Fixture{
		groupSet.openForSignups = true
		groupSet.allocationMethod = StudentSignUp
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Seq(group1, group2), StudentNotAssignedToGroup))
	}}

	@Test
	def groupstoDisplayReturnsNilForClosedSelfSignupWithNoAllocation(){ new Fixture{
		groupSet.openForSignups = false
		groupSet.allocationMethod = StudentSignUp
		getGroupsToDisplay(groupSet, unallocatedUser) should be ((Nil, StudentNotAssignedToGroup))
	}}

	@Test
	def groupsToDisplayReturnsAllocatedGroupForNonSelfSignUp(){ new Fixture {
		groupSet.allocationMethod= Manual
		getGroupsToDisplay(groupSet, allocatedUser) should be ((Seq(group1), StudentAssignedToGroup))
	}}

	@Test
	def groupsToDisplayReturnsAllocatedGroupForSelfSignUp(){ new Fixture {
		groupSet.allocationMethod= StudentSignUp
		getGroupsToDisplay(groupSet, allocatedUser) should be ((Seq(group1), StudentAssignedToGroup))
	}}

	@Test
	def viewModulesRemovesModulesWithNoGroups(){new Fixture{
		// dummy getGroupsToDisplay that always returns an empty set
		def neverReturnGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (Nil,StudentNotAssignedToGroup )
		getViewModulesForStudent(Seq(groupSet), neverReturnGroups) should be (Nil)
	}}

	@Test
	def ManualGroupSetsReleasedToStudents(){ new Fixture{
		groupSet.allocationMethod=Manual
		groupSet.releasedToStudents=false
		getGroupSetsReleasedToStudents(Seq(groupSet)) should be (Nil)
		groupSet.releasedToStudents=true
		getGroupSetsReleasedToStudents(Seq(groupSet)) should not be Nil

	}}

	@Test
	def selfSignUpGroupSetsReleasedToStudents(){ new Fixture{
		groupSet.allocationMethod=StudentSignUp
		// self signup groups should be returned - we ignore the releasedToStudents flag
		groupSet.releasedToStudents=false
		getGroupSetsReleasedToStudents(Seq(groupSet)) should not be Nil
		groupSet.releasedToStudents=true
		getGroupSetsReleasedToStudents(Seq(groupSet)) should not be Nil
	}}

	@Test
	def viewModulesConvertsModuleToViewModule(){ new Fixture{
		// dummy getGroupsToDisplay that always returns all groups in the set
		def allGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (set.groups.asScala,StudentNotAssignedToGroup )
		val viewModules: Seq[ViewModule] = getViewModulesForStudent(Seq(groupSet), allGroups)
		viewModules.size should be(1)
		viewModules.head.canManageGroups should be(false)
		val viewSets: Seq[ViewSet] = viewModules.head.setItems
		viewSets.size should be(1)
		viewSets.head.groups.map(_.group) should be (Seq(group1, group2).sorted)
	}}

	@Test
	def viewModulesGroupsByModule(){ new Fixture{
		// dummy getGroupsToDisplay that always returns all groups in the set
		def allGroups(set:SmallGroupSet):(Seq[SmallGroup],ViewerRole)  = (set.groups.asScala,StudentNotAssignedToGroup )

		// two groupsets, both for the same module
		val viewModules: Seq[ViewModule] = getViewModulesForStudent(Seq(groupSet, groupSet2), allGroups)
		viewModules.size should be(1)
		val viewSets: Seq[ViewSet] = viewModules.head.setItems
		viewSets.size should be(2)
		viewSets.head.groups.map(_.group) should be (Seq(group1, group2).sorted)
		viewSets.tail.head.groups.map(_.group) should be (Seq(group3))
	}}


}

package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.notifications.groups.SmallGroupSetChangedNotification
import uk.ac.warwick.tabula.services.{UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.JavaConverters._


class NotifiesAffectedGroupMembersTest extends TestBase {


  private trait Fixture extends SmallGroupFixture {
    val user1 = new User("user1")
    user1.setWarwickId("user1")
		user1.setUserId("user1")

    val user2 = new User("user2")
    user2.setWarwickId("user2")
		user2.setUserId("user2")

    val user3 = new User("user3")
    user3.setWarwickId("user3")
		user3.setUserId("user3")

    val user4 = new User("user4")
    user4.setWarwickId("user4")
		user4.setUserId("user4")

    val userDatabase = Seq(user1, user2, user3, user4)
		userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { _ match { case ids: Seq[String @unchecked] =>
			ids.map(id => (id, userDatabase.find {_.getWarwickId == id}.getOrElse (new AnonymousUser()))).toMap
		}}
		userLookup.getUserByUserId(any[String]) answers { _ match { case id: String @unchecked =>
			userDatabase.find {_.getUserId == id}.getOrElse (new AnonymousUser())
		}}

    val eventA: SmallGroupEvent = new SmallGroupEventBuilder().withTutors(createUserGroup(Seq("tutor1","tutor2"),identifierIsUniNumber = false)).build
    val groupA: SmallGroupBuilder = new SmallGroupBuilder()
      .withGroupName("groupA")
      .withStudents(createUserGroup(Seq("user1", "user2"),identifierIsUniNumber = true))
      .withEvents(Seq(eventA))
    val eventB: SmallGroupEvent = new SmallGroupEventBuilder().withTutors(createUserGroup(Seq("tutor3"),identifierIsUniNumber = false)).build
		val groupB: SmallGroup = new SmallGroupBuilder()
			.withGroupName("groupB")
			.withStudents(createUserGroup(Seq("user3", "user4"),identifierIsUniNumber = true))
			.withEvents(Seq(eventB))
			.build
    val groupSet: SmallGroupSetBuilder = new SmallGroupSetBuilder().withReleasedToStudents(isReleased = true).withGroups(Seq(groupA.build, groupB))

    val command: StubCommand = new StubCommand(groupSet.build, actor, userLookup)

  }
  @Test
  def createsSnapshotOfSmallGroupWhenConstructed() {
    new SmallGroupFixture {
      val command: StubCommand = new StubCommand(groupSet1, actor, userLookup)

      command.setBeforeUpdates should equal(groupSet1)
      command.setBeforeUpdates.eq(groupSet1) should be {false}

    }
  }

  @Test
  def affectedStudentsGroupsDetectsAdditions() { new Fixture {
    // add user4 to group A
    val modifiedGroupA: SmallGroup = groupA.withStudents(createUserGroup(Seq("user1","user2","user4"))).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups(user4) should be { true }
    command.hasAffectedStudentsGroups(user1) should be { false }
  }}

  @Test
  def affectedStudentsGroupsDetectsRemovals() { new Fixture {
    // remove user2 from group A
    val modifiedGroupA: SmallGroup = groupA.withStudents(createUserGroup(Seq("user1"))).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups(user2) should be { true }
    command.hasAffectedStudentsGroups(user1) should be { false }
  }}

  @Test
  def affectedStudentsGroupDetectsChangesToEvents(){new Fixture{
    val event: SmallGroupEvent = new SmallGroupEventBuilder().build
    val modifiedGroupA: SmallGroup = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    // group A - affected
    command.hasAffectedStudentsGroups(user1) should be { true }
    command.hasAffectedStudentsGroups(user2) should be { true }

    // group B - unaffected
    command.hasAffectedStudentsGroups(user3) should be { false }

  }}

  @Test
  def emitsANotificationForEachAffectedStudent() { new Fixture{

    val event: SmallGroupEvent = new SmallGroupEventBuilder().build
    val modifiedGroupA: SmallGroup = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    val notifications: Seq[Notification[SmallGroup, SmallGroupSet]] = command.emit(command.set)
    notifications.size should be(2)

		notifications.foreach{
			case n: SmallGroupSetChangedNotification => n.userLookup = userLookup
		}

    notifications.exists(_.recipients == Seq(user1)) should be { true }
    notifications.exists(_.recipients == Seq(user2)) should be { true }
    notifications.exists(_.recipients == Seq(user3)) should be { false }
  }}


  @Test
  def emitsNoNotificationsToStudentsIfGroupsetIsNotReleased() { new Fixture {
    val unreleasedGroupset: SmallGroupSet =groupSet.withReleasedToStudents(isReleased = false).build
    val cmd = new StubCommand(unreleasedGroupset, actor, userLookup)
    val event: SmallGroupEvent = new SmallGroupEventBuilder().build
    val modifiedGroupA: SmallGroup = groupA.withEvents(Seq(event)).build
    cmd.set.groups = JArrayList(modifiedGroupA, groupB)

    cmd.emit(cmd.set) should be(Nil)
  }}

  @Test
  def createsFilteredGroupsetViewForTutors() { new Fixture {

    val addedEvent: SmallGroupEvent = new SmallGroupEventBuilder().build // tutor1 is not a tutor on this event
    val addedGroup: SmallGroup =   new SmallGroupBuilder().copyOf(group1).withGroupName("addedGroup").withEvents(Seq(addedEvent)).build

    val event: SmallGroupEvent = new SmallGroupEventBuilder().build // tutor1 is not a tutor on this event
    group1.addEvent(event)
    groupSet1.groups.add(addedGroup)

    groupSet1.groups.size should be(2)
    group1.events.size should be(2)

    val filteredView: SmallGroupSet = command.tutorsEvents(groupSet1,tutor1)
    filteredView.groups.size should be(1)
    filteredView.groups.asScala.head.events.size should be(1)

  }}

	@Test
	def detectsAllocationChangesForTutors() { new Fixture {

			val group: SmallGroup = command.set.groups.asScala.find(_.name == "groupA").get
			val test: User = new User("test")
			test.setWarwickId("123")
			group.students.add(test)

			val tutor: User = group.events.head.tutors.users.head
			command.hasAffectedTutorsEvents(tutor1) should be { true }
			// should return false for an arbitrary user (worryingly this failed pre TAB-2728)
			command.hasAffectedTutorsEvents(new User()) should be { false }
//			command.hasAffectedTutorsEvents(tutor3) should be { false }
	}}


  class StubCommand(val set: SmallGroupSet, val apparentUser: User, var userLookup: UserLookupService)
    extends SmallGroupSetCommand with UserLookupComponent with NotifiesAffectedGroupMembers {
  }

}

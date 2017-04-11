package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.util.Random

class SmallGroupTest extends TestBase with Mockito {

  val event: SmallGroupEvent = newEventWithMockedServices

  val equivalentEvent: SmallGroupEvent = newEventWithMockedServices

  val notEquivalentEvent: SmallGroupEvent = newEventWithMockedServices
  notEquivalentEvent.day = DayOfWeek.Monday

	val sessionFactory: SessionFactory = smartMock[SessionFactory]
	val session: Session = smartMock[Session]
	sessionFactory.getCurrentSession returns session
	sessionFactory.openSession() returns session

	def newSmallGroupWithMockedServices: SmallGroup = {
		val group = new SmallGroup()
		group.permissionsService = mock[PermissionsService]
		group.smallGroupService = None
		group
	}

	def newEventWithMockedServices: SmallGroupEvent = {
		val event = new SmallGroupEvent()
		event.smallGroupService = None
		event.permissionsService = mock[PermissionsService]
		event
	}

  @Test
  def equivalentEventsToReturnsTrueForSameGroup() {
    val group = newSmallGroupWithMockedServices

    group.hasEquivalentEventsTo(group) should be{true}
  }

  @Test
  def equivalentEventsToReturnsTrueForGroupsWithNoEvents(){
    val group = newSmallGroupWithMockedServices
    group.hasEquivalentEventsTo(newSmallGroupWithMockedServices) should be {true}
  }

  @Test
  def equivalentEventsToReturnsTrueForGroupsWithEquivalentEvents(){

    val group = newSmallGroupWithMockedServices
    group.addEvent(event)
    val group2 = newSmallGroupWithMockedServices
    group2.addEvent(equivalentEvent)
    group.hasEquivalentEventsTo(group2) should be {true}
  }

  @Test
  def equivalentEventsToReturnsFalseForGroupsWithNonEquivalentEvents(){
    val group = newSmallGroupWithMockedServices
    group.addEvent(event)
    val group2 = newSmallGroupWithMockedServices
    group2.addEvent(notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be {false}
  }

  @Test
  def equivalentEventsToReturnsFalseForGroupsWithSubsetOfEvents(){
    val group = newSmallGroupWithMockedServices
    group.addEvent(event)
    val group2 = newSmallGroupWithMockedServices
    group2.addEvent(event)
		group2.addEvent(notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be {false}
  }

  @Test
  def duplicateCopiesAllFields(){

    val source = newSmallGroupWithMockedServices
    val clonedEvent = newEventWithMockedServices

    // can't use a mockito mock because the final equals method on GeneratedId causes mockito to
    // blow up
    val event:SmallGroupEvent = new SmallGroupEvent{
      override def duplicateTo(g: SmallGroup, transient: Boolean): SmallGroupEvent = clonedEvent
    }

    val sourceSet = new SmallGroupSet

    source.name = "name"
    source.groupSet = sourceSet
    source.permissionsService = mock[PermissionsService]

		// would be nice to use a mock here, but they don't work well with GeneratedId classes
		val studentsGroup = UserGroup.ofUniversityIds
		studentsGroup.sessionFactory = sessionFactory
		studentsGroup.addUserId("test user")
    source.students = studentsGroup

    source.id = "123"
    source.addEvent(event)
    source.maxGroupSize = 12

    val targetSet = new SmallGroupSet

    val target = source.duplicateTo(targetSet, transient = true)

    // id is not copied, otherwise it wouldn't be transient
    target.id should be(null) // Don't duplicate IDs
    target.name should be(source.name)
    target.groupSet should not be source.groupSet
    target.groupSet should be(targetSet)
    target.maxGroupSize should be(source.maxGroupSize)

    target.permissionsService should be(source.permissionsService)
    target.students should not be source.students
		target.students.hasSameMembersAs(source.students) should be{true}
    target.events.size should be(1)
    target.events.head should be(clonedEvent)

  }

	@Test
	def fullReportsFullnessWhenGroupSizeSet(){

		// set up a group with 1 member, with group size limits enabled
		val group = newSmallGroupWithMockedServices
		group.groupSet = new SmallGroupSet()
		group.students.add(new User("test") {{ setWarwickId("00000001") }})


		group.maxGroupSize = 2
		group should not be 'full

		group.maxGroupSize = 1
		group should be('full)

		group.maxGroupSize = 0
		group should be('full)

	}

	@Test
	def sortIsAlphanumeric(): Unit = {
		val group1 = Fixtures.smallGroup("Group 1")
		val group2 = Fixtures.smallGroup("group 2")
		val group3 = Fixtures.smallGroup("group 20")
		val group4 = Fixtures.smallGroup("Group 10")
		val group5 = Fixtures.smallGroup("Group 9")
		val group6 = Fixtures.smallGroup("Late group 1")

		for (i <- 1 to 10) {
			val shuffled = Random.shuffle(Seq(group1, group2, group3, group4, group5, group6))

			shuffled.sorted should be (Seq(group1, group2, group5, group4, group3, group6))
		}
	}
}

package uk.ac.warwick.tabula

import java.util.UUID

import org.joda.time.LocalTime
import org.mockito.Mockito.when
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services.{SmallGroupService, UserGroupCacheManager, UserLookupService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.collection.JavaConverters._

trait SmallGroupFixture extends Mockito {

  val requestingUser = new User

  // Note that, for mysterious reasons, SmallGroup.students is a group of users by warwick ID number, but
  // SmallGroupEvent.tutors is a group of users by user code.
  val student1 = new User
	student1.setUserId("student1")
  student1.setWarwickId("student1")

  val student2 = new User
	student2.setUserId("student2")
  student2.setWarwickId("student2")

  val tutor1 = new User
  tutor1.setUserId("tutor1")

  val tutor2 = new User
  tutor2.setUserId("tutor2")

	val tutor3 = new User
	tutor3.setUserId("tutor3")

  val userLookup: UserLookupService = smartMock[UserLookupService]
  when(userLookup.getUserByWarwickUniId(student1.getWarwickId)).thenReturn(student1)
  when(userLookup.getUserByWarwickUniId(student2.getWarwickId)).thenReturn(student2)
	when(userLookup.getUserByUserId(student1.getUserId)).thenReturn(student1)
	when(userLookup.getUserByUserId(student2.getUserId)).thenReturn(student2)
  when(userLookup.getUserByUserId(tutor1.getUserId)).thenReturn(tutor1)
  when(userLookup.getUserByUserId(tutor2.getUserId)).thenReturn(tutor2)
	when(userLookup.getUserByUserId(tutor3.getUserId)).thenReturn(tutor3)
  // UserGroup does batched lookups for users when resolving by UserId...
  when(userLookup.getUsersByUserIds(Seq(tutor1.getUserId,tutor2.getUserId).asJava)).thenReturn(Map("tutor1"->tutor1, "tutor2"->tutor2).asJava)

  val students = Seq(student1, student2)
  userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { ids => ids match {
		case ids: Seq[String @unchecked] =>
			ids.map(id => (id, students.find {_.getWarwickId == id}.getOrElse (new AnonymousUser()))).toMap
		case _ => Map()
  }}

  val actor = new User
  val recipient = new User
  recipient.setWarwickId("recipient")
	recipient.setUserId("recipient")
	when(userLookup.getUserByUserId(recipient.getUserId)).thenReturn(recipient)

  val department: Department = Fixtures.department("in")
	val academicYear = AcademicYear(2015)

  val (group1,groupSet1) = createGroupSet("A Groupset 1","small group 1",SmallGroupFormat.Lab, "la101")
	val (group2,groupSet2) = createGroupSet("A Groupset 2","small group 2",SmallGroupFormat.Seminar, "la102")
  val (group3,groupSet3) = createGroupSet("A Groupset 3","small group 3",SmallGroupFormat.Tutorial, "la103")
  val (group4,groupSet4) = createGroupSet("A Groupset 4","small group 4",SmallGroupFormat.Tutorial, "la104")
  val (group5,groupSet5) = createGroupSet("A Groupset 5","small group 5",SmallGroupFormat.Lab, "la105")

	val mockSmallGroupService: SmallGroupService = smartMock[SmallGroupService]
	mockSmallGroupService.getSmallGroupSets(department, academicYear) returns Seq(groupSet1, groupSet2, groupSet3, groupSet4, groupSet5)


  def createGroupSet(groupSetName:String, groupName:String, format: SmallGroupFormat, moduleCode:String):(SmallGroup, SmallGroupSet) = {

    val mod = new Module
    mod.code=moduleCode
    mod.name="Test module " + moduleCode
    mod.adminDepartment = department
    mod.groupSets = JArrayList()


    val students = createUserGroup(Seq(student1.getWarwickId, student2.getWarwickId), identifierIsUniNumber = true)
    val tutors = createUserGroup(Seq(tutor1.getUserId,tutor2.getUserId), identifierIsUniNumber = false)

    val event = new SmallGroupEventBuilder()
      .withTutors(tutors)
      .withStartTime(new LocalTime(12,0,0,0))
      .withDay(DayOfWeek.Monday)
      .withLocation("CMR0.1")
      .build

    val smallGroup = new SmallGroupBuilder()
      .withStudents(students)
      .withEvents(Seq(event))
      .withGroupName(groupName)
      .build

    val gs = new SmallGroupSetBuilder()
      .withId(groupSetName)
      .withName(groupSetName)
      .withFormat(format)
      .withModule(mod)
      .withGroups(Seq(smallGroup))
      .build

    (gs.groups.asScala.head, gs)
  }

  def createUserGroup(userIds:Seq[String], identifierIsUniNumber:Boolean = true): UserGroup = {
    val ug = if (identifierIsUniNumber) UserGroup.ofUniversityIds else UserGroup.ofUsercodes
    ug.userLookup = userLookup
    ug.includedUserIds = userIds
    ug
  }
}

class SmallGroupSetBuilder(){
  val template = new SmallGroupSet

  def build: SmallGroupSet = {
    val set = template.duplicateTo(transient = false)
    if (template.module != null){
      template.module.groupSets.add(set)
    }
		template.groups.asScala.foreach(g=>g.groupSet = set)
    set
  }
  def withGroups(groups:Seq[SmallGroup]):SmallGroupSetBuilder = {
    template.groups = groups.asJava
    groups.foreach(g=>g.groupSet = template)
    this
  }
	def withMembers(members:UserGroup):SmallGroupSetBuilder = {
		template.members = members
		this
	}
  def withReleasedToStudents(isReleased: Boolean): SmallGroupSetBuilder = {
    template.releasedToStudents = isReleased
    this
  }
  def withId (id:String): SmallGroupSetBuilder  = {
    template.id = id
    this
  }
  def withName(name:String): SmallGroupSetBuilder = {
    template.name = name
    this
  }
  def withFormat(format:SmallGroupFormat): SmallGroupSetBuilder = {
    template.format = format
    this
  }
  def withModule(mod:Module): SmallGroupSetBuilder = {
    template.module = mod
    this
  }
	def withAllocationMethod(method:SmallGroupAllocationMethod): SmallGroupSetBuilder = {
		template.allocationMethod = method
		this
	}
}
class SmallGroupBuilder(val template:SmallGroup = new SmallGroup){

  template.id = UUID.randomUUID.toString
  def build:SmallGroup = template.duplicateTo(template.groupSet, transient = false)

  def copyOf(group:SmallGroup):SmallGroupBuilder = {
    new SmallGroupBuilder(group.duplicateTo(group.groupSet, transient = false))
  }

  def withEvents(events: Seq[SmallGroupEvent]): SmallGroupBuilder = {
		template.events.foreach(template.removeEvent)
		events.foreach { event =>
			template.addEvent(event)
			event.group = template
		}
    this
  }
  def withStudents(members:UserGroup):SmallGroupBuilder = {
    template.students = members
    this
  }
	def withUserLookup(userLookup:UserLookupService): SmallGroupBuilder ={
		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		wireUserLookup(template.students)
		this
	}
  def withGroupName(s: String): SmallGroupBuilder = {
    template.name = s
    this
  }

}

class SmallGroupEventBuilder(){

  val template = new SmallGroupEvent

  def build: SmallGroupEvent = template.duplicateTo(template.group, transient = false)

  def withTutors(members:UserGroup):SmallGroupEventBuilder = {
    template.tutors = members
    this
  }

  def withStartTime(value: LocalTime):SmallGroupEventBuilder = {
    template.startTime = value
    this
  }

  def withDay(value: DayOfWeek):SmallGroupEventBuilder = {
    template.day = value
    this
  }

  def withLocation(s: String): SmallGroupEventBuilder = {
    template.location = NamedLocation(s)
    this
  }





}

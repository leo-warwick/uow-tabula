package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, Description, Notifies, UserAware}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.data.model.notifications.groups.OpenSmallGroupSetsOtherSignUpNotification
import uk.ac.warwick.tabula.data.model.{Department, Notification, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserGroupCacheManager, UserLookupService}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.jdk.CollectionConverters._

class OpenSmallGroupSetCommandTest extends TestBase with Mockito {

  val operator: User = mock[User]

  @Test
  def marksSelfSignupGroupAsOpen(): Unit = {
    val department = Fixtures.department("in")
    val set = new SmallGroupSet()
    set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
    val cmd = new OpenSmallGroupSet(department, Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
    cmd.applyInternal()
    set.openForSignups should be(true)
  }

  @Test
  def ignoresSetsThatAreNotSelfSignUp(): Unit = {
    val department = Fixtures.department("in")
    val set = new SmallGroupSet()
    set.allocationMethod = SmallGroupAllocationMethod.Manual
    val cmd = new OpenSmallGroupSet(department, Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
    cmd.applyInternal()
    set.openForSignups should be(false)
  }


  @Test
  def processesMultipleSets(): Unit = {
    val department = Fixtures.department("in")
    val set1 = new SmallGroupSet()
    set1.allocationMethod = SmallGroupAllocationMethod.StudentSignUp
    val set2 = new SmallGroupSet()
    set2.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

    val cmd = new OpenSmallGroupSet(department, Seq(set1, set2), operator, SmallGroupSetSelfSignUpState.Open)
    cmd.applyInternal()
    set1.openForSignups should be(true)
    set2.openForSignups should be(true)
  }

  @Test
  def returnsUpdatedSets(): Unit = {
    val department = Fixtures.department("in")
    val set = new SmallGroupSet()
    set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

    val cmd = new OpenSmallGroupSet(department, Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
    cmd.applyInternal() should be(Seq(set))
  }

  @Test
  def ignoresSetsAlreadyOpened(): Unit = {
    val dept = Fixtures.department("in")
    val set = new SmallGroupSet()
    set.openForSignups = true
    set.allocationMethod = SmallGroupAllocationMethod.StudentSignUp

    val cmd = new OpenSmallGroupSet(dept, Seq(set), operator, SmallGroupSetSelfSignUpState.Open)
    cmd.applyInternal() should be(Nil)

  }

  @Test
  def requiresUpdatePermissionsOnAllSetsToBeOpened(): Unit = {
    val dept = Fixtures.department("in")
    val set1 = new SmallGroupSet()
    val set2 = new SmallGroupSet()

    val perms = new OpenSmallGroupSetPermissions with OpenSmallGroupSetState {
      val department: Department = dept
      val applicableSets = Seq(set1, set2)
    }

    val checker = mock[PermissionsChecking]
    perms.permissionsCheck(checker)
    verify(checker, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set1)
    verify(checker, times(1)).PermissionCheck(Permissions.SmallGroups.Update, set2)
  }

  @Test
  def auditsLogsTheGroupsetsToBeOpened(): Unit = {
    val dept = Fixtures.department("in")
    val sets = Seq(new SmallGroupSet())
    val audit = new OpenSmallGroupSetAudit with OpenSmallGroupSetState {
      val eventName: String = ""
      val department: Department = dept
      val applicableSets: Seq[SmallGroupSet] = sets
    }
    val description = mock[Description]
    audit.describe(description)
    verify(description, times(1)).smallGroupSetCollection(sets)
  }

  trait NotificationFixture {
    // n.b. both the userID and the warwickID need to be set. The userID is used as the key for equals() operations, and
    // the warwickID is used as the key in UserGroup.members
    val student1 = new User("student1")
    student1.setWarwickId("student1")
    student1.setUserId("student1")
    val student2 = new User("student2")
    student2.setWarwickId("student2")
    student2.setUserId("student2")
    val student3 = new User("student3")
    student3.setWarwickId("student3")
    student3.setUserId("student3")
    val students = Seq(student1, student2, student3)

    val userLookup: UserLookupService = smartMock[UserLookupService]

    def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
      case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
      case ug: UserGroup => ug.userLookup = userLookup
    }

    userLookup.getUserByUserId(any[String]) answers { id: Any =>
      students.find(_.getUserId == id).getOrElse(new AnonymousUser)
    }

    userLookup.getUserByWarwickUniId(any[String]) answers { id: Any =>
      students.find(_.getWarwickId == id).getOrElse(new AnonymousUser)
    }

    userLookup.usersByWarwickUniIds(any[Seq[String]]) answers { arg: Any =>
      arg match {
        case ids: Seq[String@unchecked] =>
          ids.map(id => (id, students.find(_.getWarwickId == id).getOrElse(new AnonymousUser()))).toMap
      }
    }
  }

  @Test
  def notifiesEachAffectedUser(): Unit = {
    new NotificationFixture {
      val membershipService: AssessmentMembershipService = mock[AssessmentMembershipService]

      val dept = Fixtures.department("in")

      val set1 = new SmallGroupSet()
      set1.smallGroupService = None

      set1.members.knownType.includedUserIds = Set(student1.getWarwickId, student2.getWarwickId)
      wireUserLookup(set1.members)

      set1.membershipService = membershipService
      val s1: Seq[User] = set1.members.users.toSeq
      membershipService.determineMembershipUsers(set1.upstreamAssessmentGroupInfos, Some(set1.members), resitOnly = false) returns s1

      val set2 = new SmallGroupSet()
      set2.smallGroupService = None

      set2.members.knownType.includedUserIds = Set(student2.getWarwickId, student3.getWarwickId)
      wireUserLookup(set2.members)

      set2.membershipService = membershipService
      val s2: Seq[User] = set2.members.users.toSeq
      membershipService.determineMembershipUsers(set2.upstreamAssessmentGroupInfos, Some(set2.members), resitOnly = false) returns s2

      val notifier = new OpenSmallGroupSetNotifier with OpenSmallGroupSetState with UserAware {
        val department: Department = dept
        val applicableSets: Seq[SmallGroupSet] = Seq(set1, set2)
        val user: User = operator
      }

      val notifications: Seq[Notification[SmallGroupSet, Unit]] = notifier.emit(Seq(set1, set2))
      notifications.foreach {
        case n: OpenSmallGroupSetsOtherSignUpNotification => n.userLookup = userLookup
      }

      notifications.size should be(3)
      notifications.find(_.recipients.head == student1) should be(Symbol("defined"))
      notifications.find(_.recipients.head == student2) should be(Symbol("defined"))
      notifications.find(_.recipients.head == student3) should be(Symbol("defined"))

    }
  }

  @Test
  def CommandStateReportsFirstGroupset(): Unit = {
    val dept = Fixtures.department("in")
    val set = new SmallGroupSet
    val oneItem = new OpenSmallGroupSetState {
      val department: Department = dept
      val applicableSets: Seq[SmallGroupSet] = Seq(set)
    }
    oneItem.singleSetToOpen should be(set)

    // arguably, this could throw an IllegalStateException to warn you
    // that you're using a command with multiple sets in a context that only
    // expects there to be one.
    val twoItems = new OpenSmallGroupSetState {
      val department: Department = dept
      val applicableSets: Seq[SmallGroupSet] = Seq(set)
    }
    twoItems.singleSetToOpen should be(set)
  }

  @Test(expected = classOf[RuntimeException])
  def CommandStateThrowsExceptionIfAskedForSingleSetFromNone(): Unit = {
    val dept = Fixtures.department("in")
    val emptyList = new OpenSmallGroupSetState {
      val department: Department = dept
      val applicableSets: Seq[SmallGroupSet] = Seq()
    }
    emptyList.singleSetToOpen

  }

  @Test
  def openSmallGroupCommandGluesEverythingTogether(): Unit = {
    val department = Fixtures.department("in")
    val command = OpenSmallGroupSetCommand(department, Seq(new SmallGroupSet), operator, SmallGroupSetSelfSignUpState.Open)

    command should be(anInstanceOf[Appliable[SmallGroupSet]])
    command should be(anInstanceOf[Notifies[Seq[SmallGroupSet], Seq[SmallGroupSet]]])
    command should be(anInstanceOf[OpenSmallGroupSet])
    command should be(anInstanceOf[OpenSmallGroupSetAudit])
    command should be(anInstanceOf[OpenSmallGroupSetNotifier])
    command should be(anInstanceOf[OpenSmallGroupSetPermissions])

  }


}

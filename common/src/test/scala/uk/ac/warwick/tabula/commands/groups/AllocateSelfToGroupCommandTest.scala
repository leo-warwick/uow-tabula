package uk.ac.warwick.tabula.commands.groups

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{Manual, StudentSignUp}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent, UserLookupService}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{CurrentUser, Mockito, SmallGroupBuilder, SmallGroupSetBuilder, TestBase}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

class AllocateSelfToGroupCommandTest extends TestBase with Mockito {

  private trait Fixture {
    val userLookup: UserLookupService = mock[UserLookupService]
    val testGroup: SmallGroup = new SmallGroupBuilder().withUserLookup(userLookup).build
    val user = new User("abcde")
    user.setWarwickId("01234")
    val currentUser: CurrentUser = new CurrentUser(user, user)

    val userDatabase = Seq(user)
    userLookup.usersByWarwickUniIds(any[Seq[String]]) answers { ids: Any =>
      ids match {
        case ids: Seq[String@unchecked] =>
          ids.map(id => (id, userDatabase.find {
            _.getWarwickId == id
          }.getOrElse(new AnonymousUser()))).toMap
      }
    }

    val testGroupSet: SmallGroupSet = new SmallGroupSetBuilder().withId("set1").withGroups(Seq(testGroup)).build
    val allocateCommand = new AllocateSelfToGroupCommand(currentUser, testGroupSet) with SmallGroupServiceComponent {
      override val smallGroupService: SmallGroupService = smartMock[SmallGroupService]
    }
    allocateCommand.group = testGroup

    val deallocateCommand = new DeallocateSelfFromGroupCommand(currentUser, testGroupSet)
    deallocateCommand.group = testGroup
  }

  @Test
  def allocateAddsStudentToGroupMembers(): Unit = {
    new Fixture {
      allocateCommand.applyInternal()
      testGroup.students.users should contain(user)
    }
  }

  @Test
  def allocateDoesNothingIfStudentAlreadyAssigned(): Unit = {
    new Fixture {
      testGroup.students.add(user)
      testGroup.students.users should contain(user)

      allocateCommand.applyInternal()
      testGroup.students.users should contain(user)
    }
  }

  @Test
  def allocateCommandReturnsGroupset(): Unit = {
    new Fixture {
      allocateCommand.applyInternal() should be(testGroupSet)
    }
  }

  @Test
  def deallocateRemovesStudentFromGroupMembers(): Unit = {
    new Fixture {
      testGroup.students.add(user)
      deallocateCommand.applyInternal()
      testGroup.students.users should be(Set.empty)
    }
  }

  @Test
  def deallocateDoesNothingIfStudentNotAlreadyAssigned(): Unit = {
    new Fixture {
      deallocateCommand.applyInternal()
      testGroup.students.users should be(Set.empty)
    }
  }


  @Test
  def deallocateCommandReturnsGroupset(): Unit = {
    new Fixture {
      deallocateCommand.applyInternal() should be(testGroupSet)
    }
  }

  @Test
  def requiresAllocateSelfPermission(): Unit = {
    new Fixture {
      val perms = new StudentSignupCommandPermissions with StudentSignUpCommandState {
        val user = null
        val groupSet: SmallGroupSet = testGroupSet
      }
      perms.group = testGroup
      val permissionsChecking: PermissionsChecking = mock[PermissionsChecking]
      perms.permissionsCheck(permissionsChecking)
      verify(permissionsChecking, times(1)).PermissionCheck(Permissions.SmallGroups.AllocateSelf, testGroup.groupSet)
    }
  }

  private trait ValidatorFixture extends Fixture {
    val signUpValidator = new AllocateSelfToGroupValidator with StudentSignUpCommandState {
      val user: CurrentUser = null
      val groupSet: SmallGroupSet = null
    }
    val unsignUpValidator = new DeallocateSelfFromGroupValidator with StudentSignUpCommandState {
      val user: CurrentUser = null
      val groupSet: SmallGroupSet = null
    }

    signUpValidator.group = testGroup
    unsignUpValidator.group = testGroup
    val errors: Errors = mock[Errors]
    testGroup.groupSet.allocationMethod = StudentSignUp
    testGroup.groupSet.openForSignups = true
    testGroupSet.allowSelfGroupSwitching = true


  }

  @Test
  def signUpValidatesGroupIsNotFull(): Unit = {
    new ValidatorFixture {

      signUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])


      testGroup.maxGroupSize = 0
      testGroup should be(Symbol("full"))

      signUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.full")
    }
  }

  @Test
  def signUpValidatesGroupIsNotClosed(): Unit = {
    new ValidatorFixture {

      signUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])

      testGroupSet.openForSignups = false

      signUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.closed")
    }
  }

  @Test
  def signUpValidatesAllocationMethodIsStudentSignUp(): Unit = {
    new ValidatorFixture {

      signUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])

      testGroupSet.allocationMethod = Manual

      signUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.notStudentSignUp")
    }
  }

  @Test
  def unsignUpValidatesGroupAllowsSwitching(): Unit = {
    new ValidatorFixture {

      unsignUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])


      testGroupSet.allowSelfGroupSwitching = false

      unsignUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.noSwitching")
    }
  }

  @Test
  def unsignUpValidatesGroupIsNotClosed(): Unit = {
    new ValidatorFixture {

      unsignUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])

      testGroupSet.openForSignups = false

      unsignUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.closed")
    }
  }

  @Test
  def unsignUpValidatesAllocationMethodIsStudentSignUp(): Unit = {
    new ValidatorFixture {

      unsignUpValidator.validate(errors)

      verify(errors, times(0)).reject(any[String])

      testGroupSet.allocationMethod = Manual

      unsignUpValidator.validate(errors)
      verify(errors, times(1)).reject("smallGroup.notStudentSignUp")
    }
  }


}

package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, Describable, DescriptionImpl, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.services.mitcircs.{MitCircsSubmissionService, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

import scala.reflect._

class RevokeRoleCommandTest extends TestBase with Mockito {

  trait CommandTestSupport[A <: PermissionsTarget] extends RoleCommandRequestMutableRoleDefinition with RoleCommandState[A] with PermissionsServiceComponent with SecurityServiceComponent with UserLookupComponent {
    val permissionsService: PermissionsService = mock[PermissionsService]
    val securityService: SecurityService = mock[SecurityService]
    val userLookup = new MockUserLookup()
  }

  // a role with a single permission to keep things simple
  val singlePermissionsRoleDefinition = new BuiltInRoleDefinition() {
    override val getName = "test"

    override def description = "test"

    GrantsScopedPermission(
      Permissions.Department.ArrangeRoutesAndModules)

    def canDelegateThisRolesPermissions: JBoolean = false
  }

  trait Fixture {
    val department: Department = Fixtures.department("in", "IT Services")

    val command = new RevokeRoleCommandInternal(department) with CommandTestSupport[Department] with RevokeRoleCommandValidation with MitCircsSubmissionServiceComponent {
      override val mitCircsSubmissionService: MitCircsSubmissionService = smartMock[MitCircsSubmissionService]
    }
  }

  @Test def nonExistingRole {
    new Fixture {
      command.roleDefinition = singlePermissionsRoleDefinition
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (None)

      // Doesn't blow up, just a no-op
      command.applyInternal() should be(None)

      verify(command.permissionsService, times(0)).saveOrUpdate(any[GrantedRole[_]])
    }
  }

  @Test def itWorksWithExisting {
    new Fixture {
      command.roleDefinition = singlePermissionsRoleDefinition
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      command.userLookup.registerUsers("cuscav", "cusebr")

      val existing = GrantedRole(department, singlePermissionsRoleDefinition)
      existing.users.knownType.addUserId("cuscao")
      existing.users.knownType.addUserId("cuscav")
      existing.users.knownType.addUserId("cusebr")

      command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))

      val grantedRole: GrantedRole[Department] = command.applyInternal().get
      (grantedRole.eq(existing)) should be(true)

      grantedRole.roleDefinition should be(singlePermissionsRoleDefinition)
      grantedRole.users.size should be(1)
      grantedRole.users.knownType.includesUserId("cuscav") should be(false)
      grantedRole.users.knownType.includesUserId("cusebr") should be(false)
      grantedRole.users.knownType.includesUserId("cuscao") should be(true)
      grantedRole.scope should be(department)

      verify(command.permissionsService, times(1)).saveOrUpdate(existing)
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscav", classTag[Department]))
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cusebr", classTag[Department]))
    }
  }

  @Test def deletesTheRoleWhenNoUsersAreLeft {
    new Fixture {
      command.roleDefinition = singlePermissionsRoleDefinition
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")
      command.usercodes.add("cuscao")
      command.userLookup.registerUsers("cuscav", "cusebr", "cuscao")

      val existing = GrantedRole(department, singlePermissionsRoleDefinition)
      existing.users.knownType.addUserId("cuscao")
      existing.users.knownType.addUserId("cuscav")
      existing.users.knownType.addUserId("cusebr")

      command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
      command.applyInternal() should be(None)

      verify(command.permissionsService, times(1)).delete(existing)
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscav", classTag[Department]))
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cusebr", classTag[Department]))
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscao", classTag[Department]))
    }
  }

  @Test def validatePasses {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")

        val existing = GrantedRole(department, singlePermissionsRoleDefinition)
        existing.users.knownType.addUserId("cuscao")
        existing.users.knownType.addUserId("cusebr")
        existing.users.knownType.addUserId("cuscav")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns true

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(false)
      }
    }
  }

  @Test def noUsercodes {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition

        val existing = GrantedRole(department, singlePermissionsRoleDefinition)
        existing.users.knownType.addUserId("cuscao")
        existing.users.knownType.addUserId("cusebr")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns true

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("usercodes")
        errors.getFieldError.getCode should be("NotEmpty")
      }
    }
  }

  @Test def usercodeNotInGroup {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition
        command.usercodes.add("cuscav")
        command.usercodes.add("cuscao")

        val existing = GrantedRole(department, singlePermissionsRoleDefinition)
        existing.users.knownType.addUserId("cuscao")
        existing.users.knownType.addUserId("cusebr")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns true

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("usercodes")
        errors.getFieldError.getCode should be("userId.notingroup")
      }
    }
  }

  @Test def noRole {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")

        command.permissionsService.getGrantedRole(department, null) returns (None)

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("roleDefinition")
        errors.getFieldError.getCode should be("NotEmpty")
      }
    }
  }

  @Test def cantRevokeWhatYouDontHave {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition
        command.usercodes.add("cusebr")

        val existing = GrantedRole(department, singlePermissionsRoleDefinition)
        existing.users.knownType.addUserId("cuscao")
        existing.users.knownType.addUserId("cusebr")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns (Some(existing))
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns false

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("roleDefinition")
        errors.getFieldError.getCode should be("permissions.cantRevokeWhatYouDontHave")
      }
    }
  }

  @Test
  def describe {
    val dept = Fixtures.department("in")
    dept.id = "dept-id"

    val command = new RevokeRoleCommandDescription[Department] with CommandTestSupport[Department] {
      override lazy val eventName: String = "test"

      val scope: Department = dept
      val grantedRole = None
    }

    command.roleDefinition = singlePermissionsRoleDefinition
    command.usercodes.add("cuscav")
    command.usercodes.add("cusebr")

    val d = new DescriptionImpl
    command.describe(d)

    d.allProperties should be(Map(
      "scope" -> "Department[dept-id]",
      "users" -> Seq("cuscav", "cusebr"),
      "roleDefinition" -> "test"
    ))
  }

  @Test def gluesEverythingTogether {
    val department = Fixtures.department("in")
    val command = RevokeRoleCommand(department)

    command should be(anInstanceOf[Appliable[GrantedRole[Department]]])
    command should be(anInstanceOf[RoleCommandState[Department]])
    command should be(anInstanceOf[RevokeRoleCommandPermissions])
    command should be(anInstanceOf[SelfValidating])
    command should be(anInstanceOf[RevokeRoleCommandValidation])
    command should be(anInstanceOf[Describable[GrantedRole[Department]]])
  }


}

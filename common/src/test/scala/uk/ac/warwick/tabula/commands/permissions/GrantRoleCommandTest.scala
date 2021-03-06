package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, Describable, DescriptionImpl, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{BuiltInRoleDefinition, DepartmentalAdministratorRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.{UserNavigation, UserNavigationGenerator, UserNavigationGeneratorComponent}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

import scala.reflect._

class GrantRoleCommandTest extends TestBase with Mockito {

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

    val command = new GrantRoleCommandInternal(department) with CommandTestSupport[Department] with GrantRoleCommandValidation with UserNavigationGeneratorComponent {
      override val userNavigationGenerator: UserNavigationGenerator = (_: String, _: Boolean) => UserNavigation("", "")
    }
  }

  @Test def itWorksForNewRole(): Unit = {
    new Fixture {
      command.roleDefinition = DepartmentalAdministratorRoleDefinition
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      command.userLookup.registerUsers("cuscav", "cusebr")

      command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns None

      val grantedRole: GrantedRole[Department] = command.applyInternal()
      grantedRole.roleDefinition should be(DepartmentalAdministratorRoleDefinition)
      grantedRole.users.size should be(2)
      grantedRole.users.knownType.includesUserId("cuscav") should be(true)
      grantedRole.users.knownType.includesUserId("cusebr") should be(true)
      grantedRole.users.knownType.includesUserId("cuscao") should be(false)
      grantedRole.scope should be(department)

      verify(command.permissionsService, times(1)).saveOrUpdate(any[GrantedRole[Department]])
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscav", classTag[Department]))
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cusebr", classTag[Department]))
    }
  }

  @Test def itWorksWithExisting(): Unit = {
    new Fixture {
      command.roleDefinition = DepartmentalAdministratorRoleDefinition
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      command.userLookup.registerUsers("cuscav", "cusebr")

      val existing = GrantedRole(department, DepartmentalAdministratorRoleDefinition)
      existing.users.knownType.addUserId("cuscao")

      command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns Some(existing)

      val grantedRole: GrantedRole[Department] = command.applyInternal()
      grantedRole.eq(existing) should be(true)

      grantedRole.roleDefinition should be(DepartmentalAdministratorRoleDefinition)
      grantedRole.users.size should be(3)
      grantedRole.users.knownType.includesUserId("cuscav") should be(true)
      grantedRole.users.knownType.includesUserId("cusebr") should be(true)
      grantedRole.users.knownType.includesUserId("cuscao") should be(true)
      grantedRole.scope should be(department)

      verify(command.permissionsService, times(1)).saveOrUpdate(existing)
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cuscav", classTag[Department]))
      verify(command.permissionsService, atLeast(1)).clearCachesForUser(("cusebr", classTag[Department]))
    }
  }

  @Test def validatePasses(): Unit = {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")
        command.userLookup.registerUsers("cuscav", "cusebr")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns None
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns true

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(false)
      }
    }
  }

  @Test def noUsercodes(): Unit = {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition

        command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns None
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

  @Test def duplicateUsercode(): Unit = {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = singlePermissionsRoleDefinition
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")
        command.usercodes.add("cuscao")

        val existing = GrantedRole(department, singlePermissionsRoleDefinition)
        existing.users.knownType.addUserId("cuscao")

        command.permissionsService.getGrantedRole(department, singlePermissionsRoleDefinition) returns Some(existing)
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns true

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("usercodes")
        errors.getFieldError.getCode should be("userId.duplicate")
      }
    }
  }

  @Test def noPermission(): Unit = {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")
        command.userLookup.registerUsers("cuscav", "cusebr")

        command.permissionsService.getGrantedRole(department, null) returns None

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        errors.getErrorCount should be(1)
        errors.getFieldError.getField should be("roleDefinition")
        errors.getFieldError.getCode should be("NotEmpty")
      }
    }
  }

  @Test def cantGiveWhatYouDontHave(): Unit = {
    withUser("cuscav", "0672089") {
      new Fixture {
        command.roleDefinition = DepartmentalAdministratorRoleDefinition
        command.usercodes.add("cuscav")
        command.usercodes.add("cusebr")
        command.userLookup.registerUsers("cuscav", "cusebr")

        command.permissionsService.getGrantedRole(department, DepartmentalAdministratorRoleDefinition) returns None
        command.securityService.canDelegate(currentUser, Permissions.Department.ArrangeRoutesAndModules, department) returns false

        val errors = new BindException(command, "command")
        command.validate(errors)

        errors.hasErrors should be(true)
        (errors.getErrorCount >= 1) should be(true)
        errors.getFieldError.getField should be("roleDefinition")
        errors.getFieldError.getCode should be("permissions.cantGiveWhatYouDontHave")
      }
    }
  }

  @Test
  def describe(): Unit = {
    val dept = Fixtures.department("in")
    dept.id = "dept-id"

    val command = new GrantRoleCommandDescription[Department] with CommandTestSupport[Department] {
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

  @Test def gluesEverythingTogether(): Unit = {
    val department = Fixtures.department("in")
    val command = GrantRoleCommand(department)

    command should be(anInstanceOf[Appliable[GrantedRole[Department]]])
    command should be(anInstanceOf[RoleCommandState[Department]])
    command should be(anInstanceOf[GrantRoleCommandPermissions])
    command should be(anInstanceOf[SelfValidating])
    command should be(anInstanceOf[GrantRoleCommandValidation])
    command should be(anInstanceOf[Describable[GrantedRole[Department]]])
  }

}

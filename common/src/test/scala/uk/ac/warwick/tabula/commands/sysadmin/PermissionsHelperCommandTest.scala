package uk.ac.warwick.tabula.commands.sysadmin

import org.springframework.core.convert.ConversionService
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.roles.RoleBuilder.GeneratedRole
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.services.permissions.{PermissionDefinition, RoleService}
import uk.ac.warwick.userlookup.User

class PermissionsHelperCommandTest extends TestBase with Mockito {

  case object EmptyBuiltInDefinition extends BuiltInRoleDefinition {

    override def description = "Empty"

    def canDelegateThisRolesPermissions: JavaImports.JBoolean = false
  }

  val securityService: SecurityService = mock[SecurityService]
  val roleService: RoleService = mock[RoleService]
  val conversionService: ConversionService = mock[ConversionService]

  private def newCommand() = {
    val cmd = new PermissionsHelperCommand
    cmd.securityService = securityService
    cmd.roleService = roleService
    cmd.conversionService = conversionService
    cmd
  }

  @Test def justUser: Unit = {
    val user = new User("cuscav")

    val cmd = newCommand()
    cmd.user = user

    val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
    val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)

    roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (LazyList(
      pd1, pd2
    ))

    val r1 = new GeneratedRole(EmptyBuiltInDefinition, None, "r1")
    val r2 = new GeneratedRole(EmptyBuiltInDefinition, None, "r2")

    roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget], isEq(false)) returns (LazyList(
      r1, r2
    ))

    val result = cmd.applyInternal()
    verify(securityService, times(0)).can(isA[CurrentUser], isA[Permission], isA[PermissionsTarget])

    result.canDo should be(false)
    result.permissions should be(Seq(pd1, pd2))
    result.roles should be(Seq(r1, r2))
    result.resolvedScope should be(null)
    result.scopeMismatch should be(false)
    result.scopeMissing should be(true)
  }

  @Test def scopeMismatch: Unit = {
    val user = new User("cuscav")

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Archive

    val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
    val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)

    roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (LazyList(
      pd1, pd2
    ))

    val r1 = new GeneratedRole(EmptyBuiltInDefinition, None, "r1")
    val r2 = new GeneratedRole(EmptyBuiltInDefinition, None, "r2")

    roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget], isEq(false)) returns (LazyList(
      r1, r2
    ))

    securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)

    val result = cmd.applyInternal()

    result.canDo should be(false)
    result.permissions should be(Seq(pd1, pd2))
    result.roles should be(Seq(r1, r2))
    result.resolvedScope should be(null)
    result.scopeMismatch should be(true)
    result.scopeMissing should be(true)
  }


  @Test def scopeMismatchWithScopeless: Unit = {
    val user = new User("cuscav")

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.UserPicker

    val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
    val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)

    roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (LazyList(
      pd1, pd2
    ))

    val r1 = new GeneratedRole(EmptyBuiltInDefinition, None, "r1")
    val r2 = new GeneratedRole(EmptyBuiltInDefinition, None, "r2")

    roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget], isEq(false)) returns (LazyList(
      r1, r2
    ))

    securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)

    val result = cmd.applyInternal()

    result.canDo should be(false)
    result.permissions should be(Seq(pd1, pd2))
    result.roles should be(Seq(r1, r2))
    result.resolvedScope should be(null)
    result.scopeMismatch should be(false)
    result.scopeMissing should be(true)
  }

  @Test def scopeResolved: Unit = {
    val user = new User("cuscav")

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Archive
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    val dept = Fixtures.department("in")
    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (dept)

    val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
    val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)

    roleService.getExplicitPermissionsFor(isA[CurrentUser], isEq(dept)) returns (LazyList(
      pd1, pd2
    ))

    val r1 = new GeneratedRole(EmptyBuiltInDefinition, None, "r1")
    val r2 = new GeneratedRole(EmptyBuiltInDefinition, None, "r2")

    roleService.getRolesFor(isA[CurrentUser], isEq(dept), isEq(false)) returns (LazyList(
      r1, r2
    ))

    securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isEq(dept)) returns (true)

    val result = cmd.applyInternal()

    result.canDo should be(true)
    result.permissions should be(Seq(pd1, pd2))
    result.roles should be(Seq(r1, r2))
    result.resolvedScope should be(dept)
    result.scopeMismatch should be(false)
    result.scopeMissing should be(false)
  }

  @Test def cantResolveScope: Unit = {
    val user = new User("cuscav")

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Archive
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (null)

    val pd1 = PermissionDefinition(Permissions.RolesAndPermissions.Read, None, true)
    val pd2 = PermissionDefinition(Permissions.Module.Create, None, false)

    roleService.getExplicitPermissionsFor(isA[CurrentUser], isNull[PermissionsTarget]) returns (LazyList(
      pd1, pd2
    ))

    val r1 = new GeneratedRole(EmptyBuiltInDefinition, None, "r1")
    val r2 = new GeneratedRole(EmptyBuiltInDefinition, None, "r2")

    roleService.getRolesFor(isA[CurrentUser], isNull[PermissionsTarget], isEq(false)) returns (LazyList(
      r1, r2
    ))

    securityService.can(isA[CurrentUser], isEq(Permissions.Assignment.Archive), isNull[PermissionsTarget]) returns (false)

    val result = cmd.applyInternal()

    result.canDo should be(false)
    result.permissions should be(Seq(pd1, pd2))
    result.roles should be(Seq(r1, r2))
    result.resolvedScope should be(null)
    result.scopeMismatch should be(true)
    result.scopeMissing should be(true)
  }

  @Test def validatePasses: Unit = {
    val user = new User("cuscav")
    user.setFoundUser(true)

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Create
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    val dept = Fixtures.department("in")
    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (dept)

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.hasErrors should be(false)
  }

  @Test def validateNoPermissionOrScopePasses: Unit = {
    val user = new User("cuscav")
    user.setFoundUser(true)

    val cmd = newCommand()
    cmd.user = user

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.hasErrors should be(false)
  }

  @Test def validateNoUser: Unit = {
    val cmd = newCommand()
    cmd.permission = Permissions.Assignment.Create
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    val dept = Fixtures.department("in")
    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (dept)

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("user")
    errors.getFieldError.getCode should be("permissionsHelper.user.invalid")
  }

  @Test def validateNotFoundUser: Unit = {
    val user = new User("cuscav")
    user.setFoundUser(false)

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Create
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    val dept = Fixtures.department("in")
    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (dept)

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("user")
    errors.getFieldError.getCode should be("permissionsHelper.user.invalid")
  }

  @Test def validateInvalidScopeType: Unit = {
    val user = new User("cuscav")
    user.setFoundUser(true)

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Create
    cmd.scopeType = classOf[PermissionsTarget]
    cmd.scope = "in"

    conversionService.canConvert(classOf[String], classOf[PermissionsTarget]) returns (false)

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("scopeType")
    errors.getFieldError.getCode should be("permissionsHelper.scopeType.invalid")
  }

  @Test def validateInvalidScope: Unit = {
    val user = new User("cuscav")
    user.setFoundUser(true)

    val cmd = newCommand()
    cmd.user = user
    cmd.permission = Permissions.Assignment.Create
    cmd.scopeType = classOf[Department]
    cmd.scope = "in"

    val dept = Fixtures.department("in")
    conversionService.canConvert(classOf[String], classOf[Department]) returns (true)
    conversionService.convert("in", classOf[Department]) returns (null)

    val errors = new BindException(cmd, "command")
    cmd.validate(errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("scope")
    errors.getFieldError.getCode should be("permissionsHelper.scope.invalid")
  }

}

package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.convert.RoleDefinitionConverter
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.permissions.{GrantedPermission, GrantedRole}
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.roles.RoleBuilder.GeneratedRole
import uk.ac.warwick.tabula.roles.{RoleBuilder, RoleDefinition, SelectorBuiltInRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.sysadmin.GlobalPermissionsCommand._

import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters._
import scala.reflect.classTag

@Controller
@RequestMapping(Array("/sysadmin/global-permissions"))
class GlobalPermissionsController extends BaseSysadminController
  with AutowiringPermissionsServiceComponent
  with AutowiringRelationshipServiceComponent {
  validatesSelf[SelfValidating]

  @ModelAttribute("grantRoleCommand") def grantRoleCommand(): GrantRoleCommand = GlobalPermissionsCommand.grantRole()
  @ModelAttribute("revokeRoleCommand") def revokeRoleCommand(): RevokeRoleCommand = GlobalPermissionsCommand.revokeRole()
  @ModelAttribute("grantPermissionsCommand") def grantPermissionsCommand(): GrantPermissionsCommand = GlobalPermissionsCommand.grantPermissions()
  @ModelAttribute("revokePermissionsCommand") def revokePermissionsCommand(): RevokePermissionsCommand = GlobalPermissionsCommand.revokePermissions()

  @RequestMapping
  def formView: String = "sysadmin/global-permissions"

  @PostMapping(params = Array("_action=grantRole"))
  def grantRole(
    @Valid @ModelAttribute("grantRoleCommand") command: GrantRoleCommand,
    errors: Errors,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      command.apply()
      RedirectFlashing(Routes.sysadmin.globalPermissions, "flash__success" -> "flash.globalPermissions.roleGranted")
    }

  @PostMapping(params = Array("_action=revokeRole"))
  def revokeRole(
    @Valid @ModelAttribute("revokeRoleCommand") command: RevokeRoleCommand,
    errors: Errors,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      command.apply()
      RedirectFlashing(Routes.sysadmin.globalPermissions, "flash__success" -> "flash.globalPermissions.roleRevoked")
    }

  @PostMapping(params = Array("_action=grantPermissions"))
  def grantPermissions(
    @Valid @ModelAttribute("grantPermissionsCommand") command: GrantPermissionsCommand,
    errors: Errors,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      command.apply()
      RedirectFlashing(Routes.sysadmin.globalPermissions, "flash__success" -> "flash.globalPermissions.permissionsGranted")
    }

  @PostMapping(params = Array("_action=revokePermissions"))
  def revokePermissions(
    @Valid @ModelAttribute("revokePermissionsCommand") command: RevokePermissionsCommand,
    errors: Errors,
    model: ModelMap
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      command.apply()
      RedirectFlashing(Routes.sysadmin.globalPermissions, "flash__success" -> "flash.globalPermissions.permissionsRevoked")
    }

  val converter = new RoleDefinitionConverter

  implicit val defaultOrderingForRoleDefinition: Ordering[RoleDefinition] = Ordering.by[RoleDefinition, String](converter.convertLeft)

  @ModelAttribute("globalGrantedRoles")
  def globalGrantedRoles(): Map[String, GrantedRole[PermissionsTarget]] =
    permissionsService.getAllGlobalGrantedRoles
      .groupBy(_.roleDefinition)
      .map { case (defn, roles) => converter.convertLeft(defn) -> roles.head }

  @ModelAttribute("existingRoleDefinitions")
  def existingRoleDefinitions(): SortedMap[RoleDefinition, GeneratedRole] =
    SortedMap(
      permissionsService.getAllGlobalGrantedRoles
        .groupBy(_.roleDefinition)
        .map { case (defn, roles) => defn -> roles.head.build() }
        .toSeq: _*
    )

  @ModelAttribute("grantableRoleDefinitions")
  def grantableRoleDefinitions(@ModelAttribute("existingRoleDefinitions") existingRoleDefinitions: SortedMap[RoleDefinition, GeneratedRole]): SortedMap[RoleDefinition, GeneratedRole] = transactional(readOnly = true) {
    val builtInRoleDefinitions = ReflectionHelper.allBuiltInRoleDefinitions

    val relationshipTypes = relationshipService.allStudentRelationshipTypes

    val selectorBuiltInRoleDefinitions =
      ReflectionHelper.allSelectorBuiltInRoleDefinitionNames.flatMap { name =>
        SelectorBuiltInRoleDefinition.of(name, PermissionsSelector.Any[StudentRelationshipType]) +:
          relationshipTypes.map { relationshipType =>
            SelectorBuiltInRoleDefinition.of(name, relationshipType)
          }
      }

    val allDefinitions = (builtInRoleDefinitions ++ selectorBuiltInRoleDefinitions).filter(_.isAssignable)

    SortedMap(
      allDefinitions
        .filterNot { defn => existingRoleDefinitions.contains(defn) }
        .map { defn => defn -> RoleBuilder.build(defn, None, defn.getName) }: _*
    )
  }

  @ModelAttribute("existingPermissions")
  def existingPermissions(): Seq[GrantedPermission[PermissionsTarget]] =
    permissionsService.getAllGlobalGrantedPermissions.filter(!_.users.isEmpty)

  @ModelAttribute("allPermissions")
  def allPermissions(): Map[String, Seq[(String, String)]] = {
    def groupFn(p: Permission) = {
      val simpleName = Permissions.shortName(p.getClass)

      val parentName =
        if (simpleName.indexOf('.') == -1) ""
        else simpleName.substring(0, simpleName.lastIndexOf('.'))

      parentName
    }

    val relationshipTypes = relationshipService.allStudentRelationshipTypes

    ReflectionHelper.allPermissions
      .filter { p => groupFn(p).hasText }
      .sortBy { p => groupFn(p) }
      .flatMap { p =>
        if (p.isInstanceOf[SelectorPermission[_]]) {
          p +: relationshipTypes.map { relationshipType =>
            SelectorPermission.of(p.getName, relationshipType)
          }
        } else {
          Seq(p)
        }
      }
      .groupBy(groupFn)
      .map { case (key, value) =>
        key -> value.map { p =>
          val name = p match {
            case p: SelectorPermission[_] => p.toString()
            case _ => p.getName
          }

          (name, name)
        }
      }
  }
}

object GlobalPermissionsCommand {
  type GrantRoleCommand = Appliable[GrantedRole[PermissionsTarget]] with SelfValidating
  type RevokeRoleCommand = Appliable[GrantedRole[PermissionsTarget]] with SelfValidating
  type GrantPermissionsCommand = Appliable[GrantedPermission[PermissionsTarget]] with SelfValidating
  type RevokePermissionsCommand = Appliable[GrantedPermission[PermissionsTarget]] with SelfValidating

  def grantRole(): GrantRoleCommand =
    new GrantGlobalRoleCommandInternal
      with ComposableCommand[GrantedRole[PermissionsTarget]]
      with GlobalRoleCommandRequest
      with GrantGlobalRoleCommandValidation
      with GlobalPermissionsCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringUserLookupComponent
      with GlobalRoleCommandDescription {
      override lazy val eventName: String = "GrantGlobalRole"
    }

  def revokeRole(): RevokeRoleCommand =
    new RevokeGlobalRoleCommandInternal
      with ComposableCommand[GrantedRole[PermissionsTarget]]
      with GlobalRoleCommandRequest
      with RevokeGlobalRoleCommandValidation
      with GlobalPermissionsCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringUserLookupComponent
      with GlobalRoleCommandDescription {
      override lazy val eventName: String = "RevokeGlobalRole"
    }

  def grantPermissions(): GrantPermissionsCommand =
    new GrantGlobalPermissionsCommandInternal
      with ComposableCommand[GrantedPermission[PermissionsTarget]]
      with GlobalPermissionsCommandRequest
      with GrantGlobalPermissionsCommandValidation
      with GlobalPermissionsCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringUserLookupComponent
      with GlobalPermissionsCommandDescription {
      override lazy val eventName: String = "GrantGlobalPermissions"
    }

  def revokePermissions(): RevokePermissionsCommand =
    new RevokeGlobalPermissionsCommandInternal
      with ComposableCommand[GrantedPermission[PermissionsTarget]]
      with GlobalPermissionsCommandRequest
      with RevokeGlobalPermissionsCommandValidation
      with GlobalPermissionsCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringUserLookupComponent
      with GlobalPermissionsCommandDescription {
      override lazy val eventName: String = "RevokeGlobalPermissions"
    }
}

abstract class GrantGlobalRoleCommandInternal extends CommandInternal[GrantedRole[PermissionsTarget]] {
  self: GlobalRoleCommandRequest
    with PermissionsServiceComponent =>

  override def applyInternal(): GrantedRole[PermissionsTarget] = transactional() {
    val role: GrantedRole[PermissionsTarget] = permissionsService.getOrCreateGlobalGrantedRole(roleDefinition)
    usercodes.asScala.foreach(role.users.knownType.addUserId)

    permissionsService.saveOrUpdate(role)

    // For each usercode that we've added, clear the cache
    usercodes.asScala.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[PermissionsTarget]))
    }

    role
  }
}

abstract class RevokeGlobalRoleCommandInternal extends CommandInternal[GrantedRole[PermissionsTarget]] {
  self: GlobalRoleCommandRequest
    with PermissionsServiceComponent =>

  override def applyInternal(): GrantedRole[PermissionsTarget] = transactional() {
    val grantedRole: Option[GrantedRole[PermissionsTarget]] = permissionsService.getGlobalGrantedRole(roleDefinition)

    grantedRole.flatMap { role =>
      usercodes.asScala.foreach(role.users.knownType.removeUserId)

      val result = if (role.users.size == 0) {
        permissionsService.delete(role)
        None
      } else {
        permissionsService.saveOrUpdate(role)
        Some(role)
      }

      // For each usercode that we've removed, clear the cache
      usercodes.asScala.foreach { usercode => permissionsService.clearCachesForUser((usercode, classTag[PermissionsTarget])) }
      result
    }.orNull
  }
}

abstract class GrantGlobalPermissionsCommandInternal extends CommandInternal[GrantedPermission[PermissionsTarget]] {
  self: GlobalPermissionsCommandRequest
    with PermissionsServiceComponent =>

  override def applyInternal(): GrantedPermission[PermissionsTarget] = transactional() {
    val grantedPermission: GrantedPermission[PermissionsTarget] = permissionsService.getOrCreateGlobalGrantedPermission(permission, overrideType)
    usercodes.asScala.foreach(grantedPermission.users.knownType.addUserId)

    permissionsService.saveOrUpdate(grantedPermission)

    // For each usercode that we've added, clear the cache
    usercodes.asScala.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[PermissionsTarget]))
    }

    grantedPermission
  }
}

abstract class RevokeGlobalPermissionsCommandInternal extends CommandInternal[GrantedPermission[PermissionsTarget]] {
  self: GlobalPermissionsCommandRequest
    with PermissionsServiceComponent =>

  override def applyInternal(): GrantedPermission[PermissionsTarget] = transactional() {
    val grantedPermission: Option[GrantedPermission[PermissionsTarget]] = permissionsService.getGlobalGrantedPermission(permission, overrideType)

    grantedPermission.foreach { permission =>
      usercodes.asScala.foreach(permission.users.knownType.removeUserId)

      permissionsService.saveOrUpdate(permission)

      // For each usercode that we've removed, clear the cache
      usercodes.asScala.foreach { usercode => permissionsService.clearCachesForUser((usercode, classTag[PermissionsTarget])) }
    }

    grantedPermission.orNull
  }
}

trait GlobalRoleCommandRequest extends GlobalPermissionsRequest {
  var roleDefinition: RoleDefinition = _
}

trait GlobalPermissionsCommandRequest extends GlobalPermissionsRequest {
  var permission: Permission = _
  var overrideType: Boolean = _
}

trait GlobalPermissionsRequest {
  var usercodes: JList[String] = JArrayList()
}

trait GrantGlobalRoleCommandValidation extends SelfValidating {
  self: GlobalRoleCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      lazy val grantedRole: Option[GrantedRole[PermissionsTarget]] = permissionsService.getGlobalGrantedRole(roleDefinition)

      val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes", staffOnlyForADS = true) {
        override def alreadyHasCode: Boolean = usercodes.asScala.exists { u => grantedRole.exists(_.users.knownType.includesUserId(u)) }
      }
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }
  }
}

trait RevokeGlobalRoleCommandValidation extends SelfValidating {
  self: GlobalRoleCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      lazy val grantedRole: Option[GrantedRole[PermissionsTarget]] = permissionsService.getGlobalGrantedRole(roleDefinition)

      grantedRole.map(_.users).foreach { users =>
        for (code <- usercodes.asScala) {
          if (!users.knownType.includesUserId(code)) {
            errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
          }
        }
      }
    }
  }
}

trait GrantGlobalPermissionsCommandValidation extends SelfValidating {
  self: GlobalPermissionsCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      lazy val grantedPermission: Option[GrantedPermission[PermissionsTarget]] = permissionsService.getGlobalGrantedPermission(permission, overrideType)

      val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes", staffOnlyForADS = true) {
        override def alreadyHasCode: Boolean = usercodes.asScala.exists { u => grantedPermission.exists(_.users.knownType.includesUserId(u)) }
      }
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }
  }
}

trait RevokeGlobalPermissionsCommandValidation extends SelfValidating {
  self: GlobalPermissionsCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      lazy val grantedPermission: Option[GrantedPermission[PermissionsTarget]] = permissionsService.getGlobalGrantedPermission(permission, overrideType)

      grantedPermission.map(_.users).foreach { users =>
        for (code <- usercodes.asScala) {
          if (!users.knownType.includesUserId(code)) {
            errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
          }
        }
      }
    }
  }
}

trait GlobalPermissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.RolesAndPermissions.Create, PermissionsTarget.Global)
}

trait GlobalRoleCommandDescription extends Describable[GrantedRole[PermissionsTarget]] {
  self: GlobalRoleCommandRequest =>

  def describe(d: Description): Unit = d.properties(
    "users" -> usercodes.asScala,
    "roleDefinition" -> roleDefinition.getName
  )
}

trait GlobalPermissionsCommandDescription extends Describable[GrantedPermission[PermissionsTarget]] {
  self: GlobalPermissionsCommandRequest =>

  def describe(d: Description): Unit = d.properties(
    "users" -> usercodes.asScala,
    "permission" -> permission.getName
  )
}

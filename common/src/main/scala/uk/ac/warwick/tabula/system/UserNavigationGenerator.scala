package uk.ac.warwick.tabula.system

import java.time.Duration

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.marks.MarksManagementHomeCommand
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.TeachingQualityUser
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, PermissionsService, RoleService}
import uk.ac.warwick.tabula.services.{AssessmentService, CourseAndRouteService, ModuleAndDepartmentService, ProfileService, SecurityService}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{CurrentUser, Features}
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.util.cache.{CacheEntryFactory, Caches}

import scala.jdk.CollectionConverters._

case class UserNavigation(
  collapsed: String,
  expanded: String
) extends java.io.Serializable

trait UserNavigationGenerator {
  def apply(usercode: String, forceUpdate: Boolean = false): UserNavigation
}

object UserNavigationGeneratorImpl extends UserNavigationGenerator with AutowiredTextRendererComponent with AutowiringCacheStrategyComponent {

  final val NavigationTemplate = "/WEB-INF/freemarker/navigation.ftl"
  final val CacheName = "UserNavigation"
  final val CacheExpiryTime: Duration = Duration.ofHours(6)

  var moduleService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
  var routeService: CourseAndRouteService = Wire[CourseAndRouteService]
  var permissionsService: PermissionsService = Wire[PermissionsService]
  var roleService: RoleService = Wire[RoleService]
  var securityService: SecurityService = Wire[SecurityService]
  var profileService: ProfileService = Wire[ProfileService]
  var assessmentService: AssessmentService = Wire[AssessmentService]
  var userLookup: UserLookupInterface = Wire[UserLookupInterface]
  var features: Features = Wire[Features]

  private def render(user: CurrentUser): UserNavigation = {
    val homeDepartment = moduleService.getDepartmentByCode(user.apparentUser.getDepartmentCode)

    val canDeptAdmin = user.loggedIn && moduleService.departmentsWithPermission(user, Permissions.Department.Reports).nonEmpty
    val canAdmin = canDeptAdmin ||
      // Avoid doing too much work by just returning the first one of these that's true
      user.loggedIn && (
        moduleService.departmentsWithPermission(user, Permissions.Module.Administer).nonEmpty ||
          moduleService.departmentsWithPermission(user, Permissions.Route.Administer).nonEmpty ||
          moduleService.modulesWithPermission(user, Permissions.Module.Administer).nonEmpty ||
          routeService.routesWithPermission(user, Permissions.Route.Administer).nonEmpty ||
          securityService.can(user, Permissions.Department.ViewManualMembershipSummary, homeDepartment.orNull)
        )

    val canViewProfiles =
      user.isStaff ||
        user.isStudent ||
        permissionsService.getAllPermissionDefinitionsFor(user, Permissions.Profiles.ViewSearchResults).nonEmpty

    // TODO - reuse this for the new exam management stuff
    val examsEnabled = features.exams && false
    val examGridsEnabled = features.examGrids && user.isStaff &&
      (canDeptAdmin || moduleService.departmentsWithPermission(user, Permissions.Department.ExamGrids).nonEmpty)

    val canManageMitigatingCircumstances =
      user.isStaff && (
        permissionsService.getAllPermissionDefinitionsFor(user, Permissions.MitigatingCircumstancesSubmission.Read).nonEmpty ||
        roleService.hasRole(user, TeachingQualityUser())
      )

    val canManageMarksManagement =
      permissionsService.getAllPermissionDefinitionsFor(user, MarksManagementHomeCommand.AdminPermission).nonEmpty

    val modelMap = Map(
      "user" -> user,
      "canAdmin" -> canAdmin,
      "canDeptAdmin" -> canDeptAdmin,
      "canViewProfiles" -> canViewProfiles,
      "examsEnabled" -> examsEnabled,
      "examGridsEnabled" -> examGridsEnabled,
      "canManageMitigatingCircumstances" -> canManageMitigatingCircumstances,
      "canManageMarksManagement" -> canManageMarksManagement
    )

    UserNavigation(
      textRenderer.renderTemplate(NavigationTemplate, modelMap ++ Map("isCollapsed" -> true)),
      textRenderer.renderTemplate(NavigationTemplate, modelMap ++ Map("isCollapsed" -> false))
    )
  }

  val cacheEntryFactory: CacheEntryFactory[String, UserNavigation] = new CacheEntryFactory[String, UserNavigation] {
    def create(usercode: String): UserNavigation = {
      userLookup.getUserByUserId(usercode) match {
        case FoundUser(foundUser) =>
          render(new CurrentUser(foundUser, foundUser))
        case _ =>
          UserNavigation("", "")
      }
    }

    def create(keys: JList[String]): JMap[String, UserNavigation] = {
      JMap(keys.asScala.toSeq.map(id => (id, create(id))): _*)
    }

    def isSupportsMultiLookups: Boolean = true

    def shouldBeCached(response: UserNavigation): Boolean = true
  }

  private lazy val navigationCache =
    Caches.builder(CacheName, cacheEntryFactory, cacheStrategy)
      .expireAfterWrite(CacheExpiryTime)
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()

  def apply(usercode: String, forceUpdate: Boolean = false): UserNavigation = {
    if (forceUpdate) {
      navigationCache.remove(usercode)
      navigationCache.get(usercode)
    } else {
      navigationCache.get(usercode)
    }
  }

}

trait UserNavigationGeneratorComponent {
  def userNavigationGenerator: UserNavigationGenerator
}

trait DefaultUserNavigationGeneratorComponent extends UserNavigationGeneratorComponent{
  def userNavigationGenerator: UserNavigationGenerator = UserNavigationGeneratorImpl
}

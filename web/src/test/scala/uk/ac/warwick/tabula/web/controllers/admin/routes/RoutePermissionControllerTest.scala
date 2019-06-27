package uk.ac.warwick.tabula.web.controllers.admin.routes

import org.springframework.validation.{BindException, Errors}
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommand, RevokeRoleCommand, RoleCommandRequestMutableRoleDefinition, RoleCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.roles.RouteManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable

class RoutePermissionControllerTest extends TestBase with Mockito {

  val listController = new RoutePermissionController
  val addController = new RouteAddPermissionController
  val removeController = new RouteRemovePermissionController

  trait Fixture {
    val department: Department = Fixtures.department("in")
    val route: Route = Fixtures.route("i100")
    route.adminDepartment = department

    val userLookup = new MockUserLookup
    Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }

    userLookup.registerUsers("cuscav", "cusebr")
  }

  @Test def createCommands {
    Seq(listController, addController, removeController).foreach { controller =>
      new Fixture {
        val addCommand: GrantRoleCommand.Command[Route] = controller.addCommandModel(route)
        val removeCommand: RevokeRoleCommand.Command[Route] = controller.removeCommandModel(route)

        addCommand should be(anInstanceOf[Appliable[GrantedRole[Route]]])
        addCommand should be(anInstanceOf[RoleCommandState[Route]])

        removeCommand should be(anInstanceOf[Appliable[GrantedRole[Route]]])
        removeCommand should be(anInstanceOf[RoleCommandState[Route]])
      }
    }
  }

  @Test def list {
    new Fixture {
      val mav: Mav = listController.permissionsForm(route, Array(), null, null)
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be('empty)
      mav.toModel("role") should be(Some(null))
      mav.toModel("action") should be(null.asInstanceOf[String])
    }
  }

  @Test def listFromRedirect {
    new Fixture {
      val mav: Mav = listController.permissionsForm(route, Array("cuscav", "cusebr"), RouteManagerRoleDefinition, "add")
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(RouteManagerRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def add {
    new Fixture {
      val addedRole = GrantedRole(route, RouteManagerRoleDefinition)

      val command = new Appliable[GrantedRole[Route]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Route] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Route = route

        def grantedRole = Some(addedRole)

        def apply: GrantedRole[Route] = addedRole

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = addController.addPermission(command, errors)
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(RouteManagerRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def addValidationErrors {
    new Fixture {
      val addedRole = GrantedRole(route, RouteManagerRoleDefinition)

      val command = new Appliable[GrantedRole[Route]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Route] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Route = route

        def grantedRole = Some(addedRole)

        def apply(): Null = {
          fail("Should not be called")
          null
        }

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      errors.reject("fail")

      val mav: Mav = addController.addPermission(command, errors)
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

  @Test def remove {
    new Fixture {
      val removedRole = GrantedRole(route, RouteManagerRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Route]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Route] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Route = route

        def grantedRole = Some(removedRole)

        def apply: Option[GrantedRole[Route]] = Some(removedRole)

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = removeController.removePermission(command, errors)
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(RouteManagerRoleDefinition))
      mav.toModel("action") should be("remove")
    }
  }

  @Test def removeValidationErrors {
    new Fixture {
      val removedRole = GrantedRole(route, RouteManagerRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Route]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Route] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Route = route

        def grantedRole = Some(removedRole)

        def apply(): Option[GrantedRole[Route]] = {
          fail("Should not be called")
          None
        }

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")
      errors.reject("fail")

      val mav: Mav = removeController.removePermission(command, errors)
      mav.viewName should be("admin/routes/permissions")
      mav.toModel("route") should be(route)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

}

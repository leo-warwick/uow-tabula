package uk.ac.warwick.tabula.web.controllers.admin.modules

import org.springframework.validation.{BindException, Errors}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommand, RevokeRoleCommand, RoleCommandRequestMutableRoleDefinition, RoleCommandState}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable

class ModulePermissionControllerTest extends TestBase with Mockito {

  val listController = new ModulePermissionController
  val addController = new ModuleAddPermissionController
  val removeController = new ModuleRemovePermissionController

  trait Fixture {
    val department: Department = Fixtures.department("in")
    val module: Module = Fixtures.module("in101")
    module.adminDepartment = department

    val userLookup = new MockUserLookup
    Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }

    userLookup.registerUsers("cuscav", "cusebr")
  }

  @Test def createCommands {
    Seq(listController, addController, removeController).foreach { controller =>
      new Fixture {
        val addCommand: GrantRoleCommand.Command[Module] = controller.addCommandModel(module)
        val removeCommand: RevokeRoleCommand.Command[Module] = controller.removeCommandModel(module)

        addCommand should be(anInstanceOf[Appliable[GrantedRole[Module]]])
        addCommand should be(anInstanceOf[RoleCommandState[Module]])

        removeCommand should be(anInstanceOf[Appliable[GrantedRole[Module]]])
        removeCommand should be(anInstanceOf[RoleCommandState[Module]])
      }
    }
  }

  @Test def list {
    new Fixture {
      val mav: Mav = listController.permissionsForm(module, Array(), null, null)
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be('empty)
      mav.toModel("role") should be(Some(null))
      mav.toModel("action") should be(null.asInstanceOf[String])
    }
  }

  @Test def listFromRedirect {
    new Fixture {
      val mav: Mav = listController.permissionsForm(module, Array("cuscav", "cusebr"), ModuleManagerRoleDefinition, "add")
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(ModuleManagerRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def add {
    new Fixture {
      val addedRole = GrantedRole(module, ModuleManagerRoleDefinition)

      val command = new Appliable[GrantedRole[Module]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Module] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Module = module

        def grantedRole = Some(addedRole)

        def apply: GrantedRole[Module] = addedRole

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = addController.addPermission(command, errors)
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(ModuleManagerRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def addValidationErrors {
    new Fixture {
      val addedRole = GrantedRole(module, ModuleManagerRoleDefinition)

      val command = new Appliable[GrantedRole[Module]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Module] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Module = module

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
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

  @Test def remove {
    new Fixture {
      val removedRole = GrantedRole(module, ModuleManagerRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Module]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Module] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Module = module

        def grantedRole = Some(removedRole)

        def apply: Option[GrantedRole[Module]] = Some(removedRole)

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = removeController.removePermission(command, errors)
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(ModuleManagerRoleDefinition))
      mav.toModel("action") should be("remove")
    }
  }

  @Test def removeValidationErrors {
    new Fixture {
      val removedRole = GrantedRole(module, ModuleManagerRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Module]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Module] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Module = module

        def grantedRole = Some(removedRole)

        def apply(): Option[GrantedRole[Module]] = {
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
      mav.viewName should be("admin/modules/permissions")
      mav.toModel("module") should be(module)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

}

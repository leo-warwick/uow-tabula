package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.validation.{BindException, Errors}
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommand, RevokeRoleCommand, RoleCommandRequestMutableRoleDefinition, RoleCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable

class DepartmentPermissionControllerTest extends TestBase with Mockito {

  val listController = new DepartmentPermissionController
  val addController = new DepartmentAddPermissionController
  val removeController = new DepartmentRemovePermissionController

  trait Fixture {
    val department: Department = Fixtures.department("in")

    val userLookup = new MockUserLookup
    Seq(listController, addController, removeController).foreach { controller => controller.userLookup = userLookup }

    userLookup.registerUsers("cuscav", "cusebr")
  }

  @Test def createCommands: Unit = {
    Seq(listController, addController, removeController).foreach { controller =>
      new Fixture {
        val addCommand: GrantRoleCommand.Command[Department] = controller.addCommandModel(department)
        val removeCommand: RevokeRoleCommand.Command[Department] = controller.removeCommandModel(department)

        addCommand should be(anInstanceOf[Appliable[GrantedRole[Department]]])
        addCommand should be(anInstanceOf[RoleCommandState[Department]])

        removeCommand should be(anInstanceOf[Appliable[GrantedRole[Department]]])
        removeCommand should be(anInstanceOf[RoleCommandState[Department]])
      }
    }
  }

  @Test def list: Unit = {
    new Fixture {
      val mav: Mav = listController.permissionsForm(department, Array(), null, null)
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be(Symbol("empty"))
      mav.toModel("role") should be(Some(null))
      mav.toModel("action") should be(null.asInstanceOf[String])
    }
  }

  @Test def listFromRedirect: Unit = {
    new Fixture {
      val mav: Mav = listController.permissionsForm(department, Array("cuscav", "cusebr"), DepartmentalAdministratorRoleDefinition, "add")
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(DepartmentalAdministratorRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def add: Unit = {
    new Fixture {
      val addedRole = GrantedRole(department, DepartmentalAdministratorRoleDefinition)

      val command = new Appliable[GrantedRole[Department]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Department] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Department = department

        def grantedRole = Some(addedRole)

        def apply: GrantedRole[Department] = addedRole

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = addController.addPermission(command, errors)
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(DepartmentalAdministratorRoleDefinition))
      mav.toModel("action") should be("add")
    }
  }

  @Test def addValidationErrors: Unit = {
    new Fixture {
      val addedRole = GrantedRole(department, DepartmentalAdministratorRoleDefinition)

      val command = new Appliable[GrantedRole[Department]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Department] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Department = department

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
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

  @Test def remove: Unit = {
    new Fixture {
      val removedRole = GrantedRole(department, DepartmentalAdministratorRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Department]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Department] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Department = department

        def grantedRole = Some(removedRole)

        def apply: Option[GrantedRole[Department]] = Some(removedRole)

        override def validate(errors: Errors): Unit = {}
      }
      command.usercodes.add("cuscav")
      command.usercodes.add("cusebr")

      val errors = new BindException(command, "command")

      val mav: Mav = removeController.removePermission(command, errors)
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel("users") should be(mutable.Map(
        "cuscav" -> userLookup.getUserByUserId("cuscav"),
        "cusebr" -> userLookup.getUserByUserId("cusebr")
      ))
      mav.toModel("role") should be(Some(DepartmentalAdministratorRoleDefinition))
      mav.toModel("action") should be("remove")
    }
  }

  @Test def removeValidationErrors: Unit = {
    new Fixture {
      val removedRole = GrantedRole(department, DepartmentalAdministratorRoleDefinition)

      val command = new Appliable[Option[GrantedRole[Department]]] with RoleCommandRequestMutableRoleDefinition with RoleCommandState[Department] with PermissionsServiceComponent with SelfValidating {
        val permissionsService = null

        def scope: Department = department

        def grantedRole = Some(removedRole)

        def apply(): Option[GrantedRole[Department]] = {
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
      mav.viewName should be("admin/department/permissions")
      mav.toModel("department") should be(department)
      mav.toModel.contains("users") should be(false)
      mav.toModel.contains("role") should be(false)
      mav.toModel.contains("action") should be(false)
    }
  }

}

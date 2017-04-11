package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{DepartmentGrantedRole, GrantedRole}
import uk.ac.warwick.tabula.commands.permissions.{GrantRoleCommandState, RevokeRoleCommandState}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent
import uk.ac.warwick.tabula.web.Mav

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

	@Test def createCommands {
		Seq(listController, addController, removeController).foreach { controller => new Fixture {
			val addCommand: controller.GrantRoleCommand = controller.addCommandModel(department)
			val removeCommand: controller.RevokeRoleCommand = controller.removeCommandModel(department)

			addCommand should be (anInstanceOf[Appliable[GrantedRole[Department]]])
			addCommand should be (anInstanceOf[GrantRoleCommandState[Department]])

			removeCommand should be (anInstanceOf[Appliable[GrantedRole[Department]]])
			removeCommand should be (anInstanceOf[RevokeRoleCommandState[Department]])
		}}
	}

	@Test def list { new Fixture {
		val mav: Mav = listController.permissionsForm(department, Array(), null, null)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users").asInstanceOf[mutable.Map[String, User]] should be ('empty)
		mav.toModel("role") should be (Some(null))
		mav.toModel("action") should be (null.asInstanceOf[String])
	}}

	@Test def listFromRedirect { new Fixture {
		val mav: Mav = listController.permissionsForm(department, Array("cuscav", "cusebr"), DepartmentalAdministratorRoleDefinition, "add")
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(DepartmentalAdministratorRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def add { new Fixture {
		val addedRole = new DepartmentGrantedRole(department, DepartmentalAdministratorRoleDefinition)

		val command = new Appliable[GrantedRole[Department]] with GrantRoleCommandState[Department] with PermissionsServiceComponent {
			val permissionsService = null
			def scope: Department = department
			def grantedRole = Some(addedRole)
			def apply: DepartmentGrantedRole = addedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav: Mav = addController.addPermission(command, errors)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(DepartmentalAdministratorRoleDefinition))
		mav.toModel("action") should be ("add")
	}}

	@Test def addValidationErrors { new Fixture {
		val addedRole = new DepartmentGrantedRole(department, DepartmentalAdministratorRoleDefinition)

		val command = new Appliable[GrantedRole[Department]] with GrantRoleCommandState[Department] with PermissionsServiceComponent {
			val permissionsService = null
			def scope: Department = department
			def grantedRole = Some(addedRole)
			def apply(): Null = {
				fail("Should not be called")
				null
			}
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		errors.reject("fail")

		val mav: Mav = addController.addPermission(command, errors)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

	@Test def remove { new Fixture {
		val removedRole = new DepartmentGrantedRole(department, DepartmentalAdministratorRoleDefinition)

		val command = new Appliable[GrantedRole[Department]] with RevokeRoleCommandState[Department] with PermissionsServiceComponent {
			val permissionsService = null
			def scope: Department = department
			def grantedRole = Some(removedRole)
			def apply: DepartmentGrantedRole = removedRole
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")

		val mav: Mav = removeController.removePermission(command, errors)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel("users") should be (mutable.Map(
			"cuscav" -> userLookup.getUserByUserId("cuscav"),
			"cusebr" -> userLookup.getUserByUserId("cusebr")
		))
		mav.toModel("role") should be (Some(DepartmentalAdministratorRoleDefinition))
		mav.toModel("action") should be ("remove")
	}}

	@Test def removeValidationErrors { new Fixture {
		val removedRole = new DepartmentGrantedRole(department, DepartmentalAdministratorRoleDefinition)

		val command = new Appliable[GrantedRole[Department]] with RevokeRoleCommandState[Department] with PermissionsServiceComponent {
			val permissionsService = null
			def scope: Department = department
			def grantedRole = Some(removedRole)
			def apply(): Null = {
				fail("Should not be called")
				null
			}
		}
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val errors = new BindException(command, "command")
		errors.reject("fail")

		val mav: Mav = removeController.removePermission(command, errors)
		mav.viewName should be ("admin/department/permissions")
		mav.toModel("department") should be (department)
		mav.toModel.contains("users") should be (false)
		mav.toModel.contains("role") should be (false)
		mav.toModel.contains("action") should be (false)
	}}

}

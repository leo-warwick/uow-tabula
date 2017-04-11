package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.admin.{CopySmallGroupSetsCommand, CopySmallGroupSetsCommandState, CopySmallGroupSetsRequestState}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.groups.{GroupsController, GroupsDepartmentsAndModulesWithPermission}
import uk.ac.warwick.tabula.web.{BreadCrumb, Mav, Routes}

import scala.collection.JavaConverters._

abstract class CopySmallGroupSetsController extends GroupsController
	 {

	validatesSelf[SelfValidating]

	type CopySmallGroupSetsCommand =
		Appliable[Seq[SmallGroupSet]] with PopulateOnForm with CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState

	def crumbsList(command: CopySmallGroupSetsCommand): Seq[BreadCrumb]

	@ModelAttribute("academicYearChoices")
	def academicYearChoices: Seq[AcademicYear] = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(3, 1)

	private def formView(command: CopySmallGroupSetsCommand) =
		Mav("groups/admin/groups/copy").crumbsList(crumbsList(command))

	@RequestMapping
	def refresh(@ModelAttribute("copySmallGroupSetsCommand") command: CopySmallGroupSetsCommand): Mav = {
		command.populate()
		formView(command)
	}

	@RequestMapping(method = Array(POST), params = Array("action!=refresh"))
	def submit(@Valid @ModelAttribute("copySmallGroupSetsCommand") command: CopySmallGroupSetsCommand, errors: Errors): Mav = {
		if (errors.hasErrors) formView(command)
		else {
			command.apply()
			Redirect(Routes.groups.admin(command.department, command.targetAcademicYear))
		}
	}

}

@Controller
@RequestMapping(value = Array("/groups/admin/module/{module}/groups/copy"))
class CopyModuleSmallGroupSetsController extends CopySmallGroupSetsController {

	@ModelAttribute("copySmallGroupSetsCommand")
	def command(@PathVariable module: Module): CopySmallGroupSetsCommand =
		CopySmallGroupSetsCommand(mandatory(module).adminDepartment, Seq(mandatory(module)))

	override def crumbsList(cmd: CopySmallGroupSetsCommand) =
		Seq(Breadcrumbs.Department(cmd.department, cmd.targetAcademicYear), Breadcrumbs.ModuleForYear(cmd.modules.head, cmd.targetAcademicYear))

}

@Controller
@RequestMapping(value = Array("/groups/admin/department/{department}/groups/copy"))
class CopyDepartmentSmallGroupSetsController extends CopySmallGroupSetsController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with GroupsDepartmentsAndModulesWithPermission with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = CopySmallGroupSetsCommand.RequiredPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("copySmallGroupSetsCommand")
	def command(@PathVariable department: Department): CopySmallGroupSetsCommand = {
		val modules = mandatory(department).modules.asScala.filter(_.groupSets.asScala.exists { set => !set.deleted && !set.archived }).sortBy { _.code }
		CopySmallGroupSetsCommand(department, modules)
	}

	override def crumbsList(cmd: CopySmallGroupSetsCommand) =
		Seq(Breadcrumbs.Department(cmd.department, cmd.targetAcademicYear))

}

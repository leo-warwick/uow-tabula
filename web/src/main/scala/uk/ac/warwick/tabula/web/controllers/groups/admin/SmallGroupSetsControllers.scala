package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ModifySmallGroupSetCommand.Command
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.commands.scheduling.UpdateLinkedSmallGroupSetsCommand
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupMembershipStyle, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor

trait SmallGroupSetsController extends GroupsController {

  validatesSelf[SelfValidating]

  var smallGroupService: SmallGroupService = Wire[SmallGroupService]

  @ModelAttribute("ManageSmallGroupsMappingParameters") def params = ManageSmallGroupsMappingParameters

  @ModelAttribute("academicYearChoices") def academicYearChoices: Seq[AcademicYear] =
    AcademicYear.now().yearsSurrounding(2, 2)

  @ModelAttribute("allFormats") def allFormats: Seq[SmallGroupFormat with Product with Serializable] = SmallGroupFormat.members

  override final def binding[A](binder: WebDataBinder, cmd: A): Unit = {
    binder.registerCustomEditor(classOf[SmallGroupFormat], new AbstractPropertyEditor[SmallGroupFormat] {
      override def fromString(code: String): SmallGroupFormat = SmallGroupFormat.fromCode(code)

      override def toString(format: SmallGroupFormat): String = format.code
    })
    binder.registerCustomEditor(classOf[SmallGroupAllocationMethod], new AbstractPropertyEditor[SmallGroupAllocationMethod] {
      override def fromString(code: String): SmallGroupAllocationMethod = SmallGroupAllocationMethod.fromDatabase(code)

      override def toString(method: SmallGroupAllocationMethod): String = method.dbValue
    })
  }

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new"))
@Controller
class CreateSmallGroupSetController extends SmallGroupSetsController {

  type CreateSmallGroupSetCommand = Appliable[SmallGroupSet] with CreateSmallGroupSetCommandState

  @ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable module: Module, @RequestParam(value = "academicYear", required = false) academicYear: AcademicYear): Seq[DepartmentSmallGroupSet] =
    smallGroupService.getDepartmentSmallGroupSets(module.adminDepartment, Option(academicYear).getOrElse(AcademicYear.now()))

  @ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable module: Module): CreateSmallGroupSetCommand =
    ModifySmallGroupSetCommand.create(module)

  @RequestMapping
  def form(@ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand): Mav = {
    Mav("groups/admin/groups/new").crumbs(Breadcrumbs.Department(cmd.module.adminDepartment, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
  }

  private def submit(cmd: CreateSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
    if (errors.hasErrors) form(cmd)
    else {
      val set = cmd.apply()
      // store default membership at the time of creation for linked to SITS type with module and current students member query
      if (set.generateLinkedSitsQueryMembers) {
        val setWithMembers = UpdateLinkedSmallGroupSetsCommand.updateIndividualSmallGroupSet(set).apply()
        RedirectForce(route(setWithMembers))
      } else {
        RedirectForce(route(set))
      }
    }
  }

  @RequestMapping(method = Array(POST), params = Array("action=update"))
  def save(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.create)

  @RequestMapping(method = Array(POST), params = Array("action!=refresh", "action!=update"))
  def saveAndExit(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, { _ => Routes.admin(cmd.module.adminDepartment, cmd.academicYear) })

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh", "action!=update"))
  def submitAndAddStudents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddStudents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh", "action!=update"))
  def submitAndAddGroups(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddGroups)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents, "action!=refresh", "action!=update"))
  def submitAndAddEvents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddEvents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh", "action!=update"))
  def submitAndAllocate(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: CreateSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAllocate)
}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}"))
@Controller
class CreateSmallGroupSetEditPropertiesController extends SmallGroupSetsController {

  type EditSmallGroupSetCommand = Appliable[SmallGroupSet] with EditSmallGroupSetCommandState

  @ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet, @RequestParam(value = "academicYear", required = false) academicYear: AcademicYear): Seq[DepartmentSmallGroupSet] =
    smallGroupService.getDepartmentSmallGroupSets(module.adminDepartment, Option(academicYear).getOrElse(set.academicYear))

  @ModelAttribute("createSmallGroupSetCommand") def cmd(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): Command =
    ModifySmallGroupSetCommand.edit(module, set)

  private def renderForm(cmd: EditSmallGroupSetCommand, model: (String, _)*) =
    Mav("groups/admin/groups/new", model: _*).crumbs(Breadcrumbs.Department(cmd.set.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))

  @RequestMapping
  def form(@ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand): Mav = renderForm(cmd)

  private def submit(cmd: EditSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
    if (errors.hasErrors) form(cmd)
    else {
      val set = cmd.apply()
      RedirectForce(route(set))
    }
  }

  @RequestMapping(method = Array(POST), params = Array("action=update"))
  def save(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav = {
    if (!errors.hasErrors) cmd.apply()

    renderForm(cmd, "saved" -> true)
  }

  @RequestMapping(method = Array(POST), params = Array("action!=refresh", "action!=update"))
  def saveAndExit(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, { _ => Routes.admin(cmd.set.department, cmd.academicYear) })

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddStudents, "action!=refresh", "action!=update"))
  def submitAndAddStudents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddStudents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups, "action!=refresh", "action!=update"))
  def submitAndAddGroups(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddGroups)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents, "action!=refresh", "action!=update"))
  def submitAndAddEvents(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAddEvents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate, "action!=refresh", "action!=update"))
  def submitAndAllocate(@Valid @ModelAttribute("createSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.createAllocate)
}

@RequestMapping(Array("/groups/admin/module/{module}/groups/{smallGroupSet}/edit", "/groups/admin/module/{module}/groups/edit/{smallGroupSet}"))
@Controller
class EditSmallGroupSetController extends SmallGroupSetsController {

  type EditSmallGroupSetCommand = Appliable[SmallGroupSet] with EditSmallGroupSetCommandState

  @ModelAttribute("departmentSmallGroupSets") def departmentSmallGroupSets(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet, @RequestParam(value = "academicYear", required = false) academicYear: AcademicYear): Seq[DepartmentSmallGroupSet] =
    smallGroupService.getDepartmentSmallGroupSets(module.adminDepartment, Option(academicYear).getOrElse(set.academicYear))

  @ModelAttribute("editSmallGroupSetCommand") def cmd(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): Command =
    ModifySmallGroupSetCommand.edit(module, set)

  private def renderForm(cmd: EditSmallGroupSetCommand, model: (String, _)*) = {
    Mav("groups/admin/groups/edit", model: _*).crumbs(Breadcrumbs.Department(cmd.set.department, cmd.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.academicYear))
  }

  @RequestMapping
  def form(@ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand): Mav = renderForm(cmd)

  private def submit(cmd: EditSmallGroupSetCommand, errors: Errors, route: SmallGroupSet => String) = {
    if (errors.hasErrors) form(cmd)
    else {
      val set = cmd.apply()
      RedirectForce(route(set))
    }
  }

  @RequestMapping(method = Array(POST), params = Array("action=update"))
  def save(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav = {
    if (!errors.hasErrors) cmd.apply()

    renderForm(cmd, "saved" -> true)
  }

  @RequestMapping(method = Array(POST), params = Array("action!=refresh", "action!=update"))
  def saveAndExit(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, { _ => Routes.admin(cmd.set.department, cmd.academicYear) })

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddStudents, "action!=refresh", "action!=update"))
  def submitAndAddStudents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.editAddStudents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups, "action!=refresh", "action!=update"))
  def submitAndAddGroups(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.editAddGroups)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents, "action!=refresh", "action!=update"))
  def submitAndAddEvents(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.editAddEvents)

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate, "action!=refresh", "action!=update"))
  def submitAndAllocate(@Valid @ModelAttribute("editSmallGroupSetCommand") cmd: EditSmallGroupSetCommand, errors: Errors): Mav =
    submit(cmd, errors, Routes.admin.editAllocate)
}

@RequestMapping(Array("/groups/admin/module/{module}/groups/{set}/delete"))
@Controller
class DeleteSmallGroupSetController extends GroupsController {

  validatesSelf[DeleteSmallGroupSetCommand]

  @ModelAttribute("smallGroupSet") def set(@PathVariable set: SmallGroupSet): SmallGroupSet = set

  @ModelAttribute("deleteSmallGroupSetCommand") def cmd(@PathVariable module: Module, @PathVariable set: SmallGroupSet) =
    new DeleteSmallGroupSetCommand(module, set)

  @RequestMapping
  def form(cmd: DeleteSmallGroupSetCommand): Mav =
    Mav("groups/admin/groups/delete")
      .crumbs(Breadcrumbs.Department(cmd.set.department, cmd.set.academicYear), Breadcrumbs.ModuleForYear(cmd.module, cmd.set.academicYear))

  @RequestMapping(method = Array(POST))
  def submit(@Valid cmd: DeleteSmallGroupSetCommand, errors: Errors): Mav =
    if (errors.hasErrors) form(cmd)
    else {
      val set = cmd.apply()
      Redirect(Routes.admin(cmd.set.department, set.academicYear))
    }

}

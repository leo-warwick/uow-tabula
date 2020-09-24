package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.marks.{GenerateModuleResitsCommand, StudentMarks}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, Module}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

@Controller
@RequestMapping(Array("/marks/admin/module/{sitsModuleCode}/{academicYear}/{occurrence}/resits"))
class GenerateModuleResitsController extends BaseModuleMarksController
  with AutowiringProfileServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  @ModelAttribute("command")
  def command(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): GenerateModuleResitsCommand.Command =
    GenerateModuleResitsCommand(sitsModuleCode, module, academicYear, occurrence, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): Seq[BreadCrumb] = {
    val department = module.adminDepartment

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.ModuleOccurrenceConfirmMarks(sitsModuleCode, module, academicYear, occurrence, active = true),
    )
  }

  @ModelAttribute("requiresResits")
  def requiresResits(@ModelAttribute("command") command: GenerateModuleResitsCommand.Command, errors: Errors): Seq[StudentMarks] =
    command.requiresResits.sortBy(_.module.sprCode)

  @ModelAttribute("assessmentComponents")
  def assessmentComponents(@ModelAttribute("command") command: GenerateModuleResitsCommand.Command, errors: Errors): Seq[AssessmentComponent] =
    command.requiresResits.flatMap(_.components.keys).distinct.sortBy(_.sequence)

  @ModelAttribute("canUpdateResits")
  def canUpdateResits(@ModelAttribute("command") command: GenerateModuleResitsCommand.Command, errors: Errors): Boolean =
    command.canUpdateResits

  private val formView: String = "marks/admin/modules/resits"

  @RequestMapping(params = Array("!confirm"))
  def preview(@ModelAttribute("command") cmd: GenerateModuleResitsCommand.Command, errors: Errors): String = {
    cmd.populate()
    formView
  }

  @RequestMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: GenerateModuleResitsCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("module") module: Module,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      val results = cmd.apply()
      val adminHome = Routes.marks.Admin.home(module.adminDepartment, academicYear)
      if (results.nonEmpty) RedirectFlashing(adminHome, "flash__success" -> "flash.module.resitsCreated")
      else RedirectFlashing(adminHome)
    }

}

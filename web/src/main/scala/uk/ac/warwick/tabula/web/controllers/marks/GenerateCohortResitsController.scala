package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.GenerateCohortResitsCommand
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.{Attempt, Department}
import uk.ac.warwick.tabula.web.Routes

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/cohort/resits"))
class GenerateCohortResitsController extends BaseCohortController {

  validatesSelf[SelfValidating]

  override val selectCourseAction: (Department, AcademicYear) => String = Routes.marks.Admin.Cohorts.resits
  override val selectCourseActionLabel: String = "Generate resits"

  @ModelAttribute("resitsCommand")
  def resitsCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, currentUser: CurrentUser): GenerateCohortResitsCommand.Command =
    GenerateCohortResitsCommand(department, academicYear, currentUser)

  @ModelAttribute("attempts")
  def attempts: Seq[Attempt] = Attempt.values


  @GetMapping(params = Array("courseSelected"))
  def summary(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @ModelAttribute("resitsCommand") resitsCommand: GenerateCohortResitsCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = getCohort(selectCourseCommand, selectCourseErrors, resitsCommand, model, department, academicYear)(resitsCommand => {
    if (!errors.hasErrors) resitsCommand.populate()
    model.addAttribute("requiresResit", resitsCommand.requiresResit)
    model.addAttribute("entitiesBySprCode", resitsCommand.entitiesBySprCode)
    model.addAttribute("breadcrumbs", Seq(MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)))
    model.addAttribute("secondBreadcrumbs", academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)))
    "marks/admin/resits"
  })

  @PostMapping
  def save(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("resitsCommand") resitsCommand: GenerateCohortResitsCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    getCohort(selectCourseCommand, selectCourseErrors, resitsCommand, model, department, academicYear)(resitsCommand => {
      if (errors.hasErrors) {
        model.addAttribute("flash__error", "flash.hasErrors")
        summary(selectCourseCommand, selectCourseErrors, resitsCommand, errors, model, department, academicYear)
      } else {
        resitsCommand.apply()
        RedirectFlashing(Routes.marks.Admin.home(department, academicYear), "flash__success" -> "flash.cohort.resitsCreated")
      }
    })

}

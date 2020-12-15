package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.ExamBoardOutcomesCommand
import uk.ac.warwick.tabula.data.model.{ActualProgressionDecision, Department, ProgressionDecisionBoard, ProgressionDecisionLevel}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}


@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/cohort/outcomes"))
class ExamBoardOutcomesController extends BaseCohortController {

  validatesSelf[SelfValidating]

  override val selectCourseAction: (Department, AcademicYear) => String = Routes.marks.Admin.Cohorts.examBoardOutcomes
  override val selectCourseActionLabel: String = "Record outcomes"

  @ModelAttribute("examBoardOutcomesCommand")
  def examBoardOutcomesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, currentUser: CurrentUser): ExamBoardOutcomesCommand.Command =
    ExamBoardOutcomesCommand(department, academicYear, currentUser)

  @ModelAttribute("decisions")
  def decisions(): Map[(ProgressionDecisionBoard, ProgressionDecisionLevel), Seq[ActualProgressionDecision]] = ActualProgressionDecision.bySessionAndLevel

  @GetMapping(params = Array("courseSelected"))
  def summary(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("examBoardOutcomesCommand") examBoardOutcomesCommand: ExamBoardOutcomesCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(examBoardOutcomesCommand => {
    if (!errors.hasErrors) examBoardOutcomesCommand.populate()
    model.addAttribute("entities", examBoardOutcomesCommand.studentDecisionRecords)
    model.addAttribute("breadcrumbs", Seq(MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)))
    model.addAttribute("secondBreadcrumbs", academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)))
    "marks/admin/outcomes"
  })

  @PostMapping(params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("examBoardOutcomesCommand") examBoardOutcomesCommand: ExamBoardOutcomesCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(examBoardOutcomesCommand => {
    examBoardOutcomesCommand.entities = selectCourseCommand.apply()
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      summary(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, errors, model, department, academicYear)
    } else {
      val changes = examBoardOutcomesCommand.studentsToRecord.flatMap { case (sprCode, item) =>
        val record = examBoardOutcomesCommand.studentDecisionRecords(sprCode)
        val unchanged = record.flatMap(_.existingRecordedDecision).exists(rd =>
          rd.decision == item.decision &&
          rd.notes == item.notes &&
          rd.minutes == item.minutes
        )
        if (unchanged) None else Some(sprCode -> record)
      }
      model.addAttribute("entities", examBoardOutcomesCommand.studentDecisionRecords)
      model.addAttribute("changes", changes)
      model.addAttribute("notifications", changes.filter(_._2.flatMap(_.existingRecordedDecision).isDefined))
      model.addAttribute("breadcrumbs", Seq(MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)))
      model.addAttribute("secondBreadcrumbs", academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)))
      "marks/admin/outcomes-preview"
    }
  })


  @PostMapping(params = Array("confirm"))
  def recordOutcomes(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("examBoardOutcomesCommand") examBoardOutcomesCommand: ExamBoardOutcomesCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(examBoardOutcomesCommand => {
      if (errors.hasErrors) {
        model.addAttribute("flash__error", "flash.hasErrors")
        summary(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, errors, model, department, academicYear)
      } else {
        examBoardOutcomesCommand.apply()
        RedirectFlashing(Routes.marks.Admin.home(department, academicYear), "flash__success" -> "flash.cohort.outcomesRecorded")
      }
    })

}

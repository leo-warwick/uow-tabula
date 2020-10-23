package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.ExamBoardOutcomesCommand
import uk.ac.warwick.tabula.data.model.{ActualProgressionDecision, Department}
import uk.ac.warwick.tabula.marks.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}


@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/cohort/outcomes"))
class ExamBoardOutcomesController extends BaseCohortController {

  validatesSelf[SelfValidating]

  override val selectCourseAction: (Department, AcademicYear) => String = Routes.Admin.Cohorts.examBoardOutcomes
  override val selectCourseActionLabel: String = "Record outcomes"
  override val selectCourseActionTitle: String = selectCourseActionLabel

  @ModelAttribute("examBoardOutcomesCommand")
  def examBoardOutcomesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, currentUser: CurrentUser): ExamBoardOutcomesCommand.Command =
    ExamBoardOutcomesCommand(department, academicYear, currentUser)

  @ModelAttribute("decisions")
  def decisions(): Seq[ActualProgressionDecision] = ActualProgressionDecision.values

  @GetMapping(params = Array("!courseSelected"))
  def summary(
    @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): String = selectCourseRender(model, department, academicYear)


  def getCohort(
    selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    examBoardOutcomesCommand: ExamBoardOutcomesCommand.Command,
    model: ModelMap,
    department: Department,
    academicYear: AcademicYear,
  )(fn: () => String): String = {
    val cohort = selectCourseCommand.apply()
    if (cohort.isEmpty) {
      selectCourseErrors.reject("examGrid.noStudents")
      selectCourseRender(model, department, academicYear)
    } else {
      val cohort = selectCourseCommand.apply()
      if (cohort.isEmpty) {
        selectCourseErrors.reject("examGrid.noStudents")
        selectCourseRender(model, department, academicYear)
      } else {
        examBoardOutcomesCommand.entities = cohort
        fn()
      }
    }
  }

  @GetMapping(params = Array("courseSelected"))
  def summary(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("examBoardOutcomesCommand") examBoardOutcomesCommand: ExamBoardOutcomesCommand.Command,
    errors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(() => {
    if (!errors.hasErrors) examBoardOutcomesCommand.populate()
    model.addAttribute("entities", examBoardOutcomesCommand.studentDecisionRecords)
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
  ): String = getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(() => {
    examBoardOutcomesCommand.entities = selectCourseCommand.apply()
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      summary(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, errors, model, department, academicYear)
    } else {
      val changes = examBoardOutcomesCommand.studentsToRecord.flatMap { case (sprCode, item) =>
        val record = examBoardOutcomesCommand.studentDecisionRecords(sprCode)
        val unchanged = record.flatMap(_.existingRecordedDecision).exists(rd => rd.decision == item.decision && rd.notes == item.notes)
        if (unchanged) None else Some(sprCode -> record)
      }
      model.addAttribute("entities", examBoardOutcomesCommand.studentDecisionRecords)
      model.addAttribute("changes", changes)
      model.addAttribute("notifications", changes.filter(_._2.flatMap(_.existingRecordedDecision).isDefined))
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
  )(implicit redirectAttributes: RedirectAttributes): String = getCohort(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, model, department, academicYear)(() => {
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      summary(selectCourseCommand, selectCourseErrors, examBoardOutcomesCommand, errors, model, department, academicYear)
    } else {
      examBoardOutcomesCommand.apply()
      RedirectFlashing(Routes.Admin.home(department, academicYear), "flash__success" -> "flash.cohort.outcomesRecorded")
    }
  })

}

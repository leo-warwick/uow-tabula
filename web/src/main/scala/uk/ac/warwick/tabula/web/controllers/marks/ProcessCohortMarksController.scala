package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.ModuleOccurrenceCommands.SprCode
import uk.ac.warwick.tabula.commands.marks.ProcessCohortMarksCommand
import uk.ac.warwick.tabula.data.model.{Department, MarkState, ModuleResult}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.SortedMap
import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/cohort"))
class ProcessCohortMarksController extends BaseCohortController
  with StudentModuleMarkRecordNotificationDepartment {

  validatesSelf[SelfValidating]

  override val selectCourseAction: (Department, AcademicYear) => String = Routes.marks.Admin.Cohorts.processMarks
  override val selectCourseActionLabel: String = "View marks"
  override val selectCourseActionTitle: String = "Process marks"

  @ModelAttribute("processCohortMarksCommand")
  def processCohortMarksCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, currentUser: CurrentUser): ProcessCohortMarksCommand.Command =
    ProcessCohortMarksCommand(department, academicYear, currentUser)

  @ModelAttribute("moduleResults")
  def moduleResults(): Seq[ModuleResult] = ModuleResult.values

  @ModelAttribute("moduleResultsDescriptions")
  def moduleResultsDescriptions(): Map[String, String] = ModuleResult.values.map(mr => mr.dbValue -> mr.description).toMap

  @GetMapping(Array("process"))
  def processSummary(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @Valid @ModelAttribute("processCohortMarksCommand") processCohortMarksCommand: ProcessCohortMarksCommand.Command,
    processCohortMarksErrors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = {
    if (selectCourseErrors.hasErrors) {
      selectCourseRender(model, department, academicYear)
    } else {
      val cohort = selectCourseCommand.apply()
      if (cohort.isEmpty) {
        selectCourseErrors.reject("examGrid.noStudents")
        selectCourseRender(model, department, academicYear)
      } else {
        processCohortMarksCommand.entities = cohort
        processCohortMarksCommand.fetchValidGrades()
        if (!processCohortMarksErrors.hasErrors) {
          processCohortMarksCommand.populate()
        }
        model.addAttribute("entities", processCohortMarksCommand.entitiesBySprCode)
        model.addAttribute("recordsByStudent", processCohortMarksCommand.recordsByStudent)
        model.addAttribute("breadcrumbs", Seq(MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)))
        model.addAttribute("secondBreadcrumbs", academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)))
        "marks/admin/process"
      }
    }
  }


  @PostMapping(path = Array("process"), params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @ModelAttribute("processCohortMarksCommand") processCohortMarksCommand: ProcessCohortMarksCommand.Command,
    processCohortMarksErrors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): String = {
    val cohort = selectCourseCommand.apply()
    if (cohort.isEmpty) {
      selectCourseErrors.reject("examGrid.noStudents")
      selectCourseRender(model, department, academicYear)
    } else {
      processCohortMarksCommand.entities = selectCourseCommand.apply()
      processCohortMarksCommand.validate(processCohortMarksErrors)
      if (processCohortMarksErrors.hasErrors) {
        model.addAttribute("flash__error", "flash.hasErrors")
        processSummary(selectCourseCommand, selectCourseErrors, processCohortMarksCommand, processCohortMarksErrors, model, department, academicYear)
      } else {
        val changes: Map[SprCode, Seq[(StudentModuleMarkRecord, Boolean)]] = processCohortMarksCommand.students.asScala.map { case (sprCode, studentModules) =>
          sprCode -> studentModules
            .asScala
            .values
            .filter(_.process)
            .flatMap { markItem =>
              val studentModuleMarkRecord = processCohortMarksCommand.studentModuleMarkRecords(markItem.moduleCode)(markItem.occurrence)
                .find(_.sprCode == markItem.sprCode).get

              val isNotNotifiableChange =
                !markItem.comments.hasText &&
                  ((!markItem.mark.hasText && studentModuleMarkRecord.mark.isEmpty) || studentModuleMarkRecord.mark.map(_.toString).contains(markItem.mark)) &&
                  ((!markItem.grade.hasText && studentModuleMarkRecord.grade.isEmpty) || studentModuleMarkRecord.grade.contains(markItem.grade)) &&
                  ((!markItem.result.hasText && studentModuleMarkRecord.result.isEmpty) || studentModuleMarkRecord.result.map(_.dbValue).contains(markItem.result))

              // Mark and grade and result haven't changed and no comment and not out of sync (we always re-push out of sync records)
              if (
                !studentModuleMarkRecord.outOfSync &&
                  (studentModuleMarkRecord.markState.contains(MarkState.Agreed) || studentModuleMarkRecord.agreed) &&
                  isNotNotifiableChange
              ) None else Some(studentModuleMarkRecord -> !isNotNotifiableChange)
            }.toSeq
        }.filterNot(_._2.isEmpty).toMap
        model.addAttribute("entities", processCohortMarksCommand.entitiesBySprCode)
        model.addAttribute("changes", SortedMap(changes.view.mapValues(_.map(_._1)).toSeq.sortBy(_._1): _*))
        model.addAttribute("notificationDepartments", departmentalStudents(changes.values.flatten.filter(_._2).map(_._1).toSeq))
        model.addAttribute("breadcrumbs", Seq(MarksBreadcrumbs.Admin.HomeForYear(department, academicYear, active = true)))
        model.addAttribute("secondBreadcrumbs", academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.home(department, _)))
        "marks/admin/process_preview"
      }
    }
  }


  @PostMapping(path = Array("process"), params = Array("confirm"))
  def processMarks(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    selectCourseErrors: Errors,
    @ModelAttribute("processCohortMarksCommand") processCohortMarksCommand: ProcessCohortMarksCommand.Command,
    processCohortMarksErrors: Errors,
    model: ModelMap,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String = {
    processCohortMarksCommand.entities = selectCourseCommand.apply()
    processCohortMarksCommand.validate(processCohortMarksErrors)
    if (processCohortMarksErrors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      processSummary(selectCourseCommand, selectCourseErrors, processCohortMarksCommand, processCohortMarksErrors, model, department, academicYear)
    } else {
      processCohortMarksCommand.apply()
      RedirectFlashing(Routes.marks.Admin.home(department, academicYear), "flash__success" -> "flash.cohort.marksProcessed")
    }
  }
}

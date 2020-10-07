package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand.{ModuleMarkCalculation, StudentModuleMarksItem}
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.{CalculateModuleMarksCommand, RecordedModuleRegistrationNotificationDepartment}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.{BreadCrumb, Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException, SprCode}

import scala.jdk.CollectionConverters._

trait StudentModuleMarkRecordNotificationDepartment extends RecordedModuleRegistrationNotificationDepartment
  with AutowiringProfileServiceComponent {
  def departmentalStudents(studentMarks: Seq[StudentModuleMarkRecord]): Map[Department, Seq[String]] = {
    studentMarks.filter(_.history.nonEmpty)
      .map { mark => (mark, notificationDepartment(mark)) }
      .filter(_._2.nonEmpty)
      .groupBy(_._2.get)
      .map { case (d, rmrWithDeptList) => d -> rmrWithDeptList.map(data => SprCode.getUniversityId(data._1.sprCode)) }
      .view.mapValues(_.distinct).toMap
  }
}

@Controller
@RequestMapping(Array("/marks/admin/module/{sitsModuleCode}/{academicYear}/{occurrence}/marks"))
class CalculateModuleMarksController extends BaseModuleMarksController
  with AutowiringJobServiceComponent
  with AutowiringMaintenanceModeServiceComponent with StudentModuleMarkRecordNotificationDepartment {

  @ModelAttribute("command")
  def command(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): CalculateModuleMarksCommand.Command =
    CalculateModuleMarksCommand(sitsModuleCode, module, academicYear, occurrence, user)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable sitsModuleCode: String, @ModelAttribute("module") module: Module, @PathVariable academicYear: AcademicYear, @PathVariable occurrence: String): Seq[BreadCrumb] = {
    val department = module.adminDepartment

    Seq(
      MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
      MarksBreadcrumbs.Admin.ModuleOccurrenceRecordMarks(sitsModuleCode, module, academicYear, occurrence, active = true),
    )
  }

  @ModelAttribute("studentModuleMarkRecords")
  def studentModuleMarkRecords(@ModelAttribute("command") command: CalculateModuleMarksCommand.Command): Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)] =
    command.studentModuleMarkRecordsAndCalculations

  @ModelAttribute("assessmentComponents")
  def assessmentComponents(@ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)]): Seq[AssessmentComponent] =
    studentModuleMarkRecords.flatMap(_._2.keySet).distinct.sortBy(_.sequence)

  @ModelAttribute("isGradeValidation")
  def isGradeValidation(@ModelAttribute("module") module: Module): Boolean =
    module.adminDepartment.assignmentGradeValidation

  @ModelAttribute("moduleResults")
  def moduleResults(): Seq[ModuleResult] = ModuleResult.values

  private val formView: String = "marks/admin/modules/calculate"

  /**
   * Upon loading the form, trigger a skippable import if:
   *
   * - Any of the student mark records is outOfSync; or
   * - The most recent import date was more than 5 minutes ago
   */
  @RequestMapping
  def triggerImportIfNecessary(
    @ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)],
    @ModelAttribute("oldestImport") oldestImport: Option[DateTime],
    @PathVariable academicYear: AcademicYear,
    model: ModelMap,
    @ModelAttribute("command") command: CalculateModuleMarksCommand.Command,
  ): String =
    if (!maintenanceModeService.enabled && (studentModuleMarkRecords.exists(_._1.outOfSync) || oldestImport.exists(_.isBefore(DateTime.now.minusHours(3))))) {
      forceImport(studentModuleMarkRecords, academicYear, model)
    } else {
      command.populate()
      formView
    }

  @RequestMapping(Array("/import"))
  def forceImport(
    @ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)],
    @PathVariable academicYear: AcademicYear,
    model: ModelMap,
  ): String = {
    val sprCodes = studentModuleMarkRecords.map(_._1.sprCode)
    val members = sprCodes.flatMap(profileService.getStudentCourseDetailsBySprCode).map(_.student)
    val universityIds = members.map(_.universityId).distinct

    stopOngoingImportForStudents(universityIds)

    val jobInstance = jobService.add(Some(user), ImportMembersJob(universityIds, Seq(academicYear)))

    model.addAttribute("jobId", jobInstance.id)
    model.addAttribute("jobProgress", jobInstance.progress)
    model.addAttribute("jobStatus", jobInstance.status)

    "marks/admin/modules/job-progress"
  }

  @ModelAttribute("studentLastImportDates")
  def studentLastImportDates(@ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)]): Seq[(String, DateTime)] = {
    val sprCodes = studentModuleMarkRecords.map(_._1.sprCode)
    val members = sprCodes.flatMap(profileService.getStudentCourseDetailsBySprCode).map(_.student)

    val studentLastImportDates =
      members.map(m =>
        (m.fullName.getOrElse(m.universityId), Option(m.lastImportDate).getOrElse(new DateTime(0)))
      ).sortBy(_._2)

    studentLastImportDates
  }

  @ModelAttribute("oldestImport")
  def oldestImport(@ModelAttribute("studentLastImportDates") studentLastImportDates: Seq[(String, DateTime)]): Option[DateTime] =
    studentLastImportDates.headOption.map { case (_, datetime) => datetime }

  private def stopOngoingImportForStudents(universityIds: Seq[String]): Unit =
    jobService.jobDao.listRunningJobs
      .filter(_.jobType == ImportMembersJob.identifier)
      .filter(_.getStrings(ImportMembersJob.MembersKey).toSet == universityIds.toSet)
      .foreach(jobService.kill)

  @PostMapping(Array("/progress"))
  def jobProgress(@RequestParam jobId: String): Mav =
    jobService.getInstance(jobId).map(jobInstance =>
      Mav(new JSONView(Map(
        "id" -> jobInstance.id,
        "status" -> jobInstance.status,
        "progress" -> jobInstance.progress,
        "finished" -> jobInstance.finished
      ))).noLayout()
    ).getOrElse(throw new ItemNotFoundException)

  // For people who hit the back button back to the /skip-import page or refresh it
  @RequestMapping(Array("/skip-import"))
  def skipImportRefresh(@ModelAttribute("command") command: CalculateModuleMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping(Array("/skip-import"))
  def skipImport(@RequestParam jobId: String, @ModelAttribute("command") command: CalculateModuleMarksCommand.Command): String = {
    jobService.getInstance(jobId)
      .filterNot(_.finished)
      .filter(_.jobType == ImportMembersJob.identifier)
      .foreach(jobService.kill)

    command.populate()
    formView
  }

  @RequestMapping(Array("/import-complete"))
  def importComplete(@ModelAttribute("command") command: CalculateModuleMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping(params = Array("!confirm"))
  def preview(
    @Valid @ModelAttribute("command") cmd: CalculateModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("studentModuleMarkRecords") studentModuleMarkRecords: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], Option[ModuleRegistration], ModuleMarkCalculation)],
    @PathVariable sitsModuleCode: String,
    @PathVariable academicYear: AcademicYear,
    @PathVariable occurrence: String
  ): String =
    if (errors.hasErrors) {
      if (errors.hasFieldErrors("file")) {
        model.addAttribute("from_origin", "upload")
      } else {
        model.addAttribute("from_origin", "webform")
      }
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      // Filter down to just changes
      val changes: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], ModuleMarkCalculation, StudentModuleMarksItem)] =
        cmd.students.asScala.values.toSeq.filter(_.calculate).flatMap { student =>
          // We know the .get is safe because it's validated
          val (studentModuleMarkRecord, componentMarks, mr, calculation) = studentModuleMarkRecords.find(_._1.sprCode == student.sprCode).get

          // Mark and grade and result haven't changed and no comment and not out of sync (we always re-push out of sync records)
          if (
            !studentModuleMarkRecord.outOfSync &&
            !student.comments.hasText &&
            ((!student.mark.hasText && studentModuleMarkRecord.mark.isEmpty) || studentModuleMarkRecord.mark.map(_.toString).contains(student.mark)) &&
            ((!student.grade.hasText && studentModuleMarkRecord.grade.isEmpty) || studentModuleMarkRecord.grade.contains(student.grade)) &&
            ((!student.result.hasText && studentModuleMarkRecord.result.isEmpty) || studentModuleMarkRecord.result.map(_.dbValue).contains(student.result))
          ) None else Some((studentModuleMarkRecord, componentMarks, calculation, student))
        }

      model.addAttribute("changes", changes)
      model.addAttribute("notificationDepartments", departmentalStudents(changes.map(_._1)))
      model.addAttribute("returnTo", getReturnTo(s"${Routes.marks.Admin.ModuleOccurrences.recordMarks(sitsModuleCode, academicYear, occurrence)}/skip-import"))

      "marks/admin/modules/calculate_preview"
    }

  @PostMapping(params = Array("confirm=true"))
  def save(
    @Valid @ModelAttribute("command") cmd: CalculateModuleMarksCommand.Command,
    errors: Errors,
    model: ModelMap,
    @ModelAttribute("module") module: Module,
    @PathVariable academicYear: AcademicYear,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("from_origin", "webform")
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      cmd.apply()

      RedirectFlashing(Routes.marks.Admin.home(module.adminDepartment, academicYear), "flash__success" -> "flash.module.marksRecorded")
    }

}

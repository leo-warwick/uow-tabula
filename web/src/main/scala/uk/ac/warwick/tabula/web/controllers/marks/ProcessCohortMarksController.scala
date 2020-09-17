package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PathVariable, PostMapping, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, GenerateExamGridSelectCourseCommand, GenerateExamGridSelectCourseCommandRequest, GenerateExamGridSelectCourseCommandState}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/cohort"))
class ProcessCohortMarksController extends BaseController
  with DepartmentScopedController with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState with SelfValidating


  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    benchmarkTask("activeDepartment") {
      retrieveActiveDepartment(Option(department))
    }

  override def departmentPermission: Permission = Permissions.Feedback.Manage

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    benchmarkTask("activeAcademicYear") {
      retrieveActiveAcademicYear(Option(academicYear))
    }

  validatesSelf[SelfValidating]

  @ModelAttribute("selectCourseCommand")
  def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    benchmarkTask("selectCourseCommand") {
      GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear), permitRoutesFromRootDepartment = securityService.can(user, departmentPermission, department.rootDepartment))
    }

  @GetMapping
  def selectCourseRender(
    @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = {
      Mav("marks/admin/selectCourse")
  }

  @GetMapping(Array("process"))
  def processSummary(
    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
  ): Mav = {
    if (errors.hasErrors) {
      selectCourseRender(selectCourseCommand, department, academicYear)
    } else {
      val cohort = selectCourseCommand.apply()
      if (cohort.isEmpty) {
        errors.reject("examGrid.noStudents")
        selectCourseRender(selectCourseCommand, department, academicYear)
      } else {
        val students = cohort.map(e => {
          e -> e.validYears.lastOption.map(_._2)
        }).toMap
        Mav("marks/admin/process", "students" -> students)
      }
    }
  }

//  @PostMapping(Array("process"))
//  def processMarks(
//    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
//    @Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
//    errors: Errors,
//    @PathVariable department: Department,
//    @PathVariable academicYear: AcademicYear,
//  )


}

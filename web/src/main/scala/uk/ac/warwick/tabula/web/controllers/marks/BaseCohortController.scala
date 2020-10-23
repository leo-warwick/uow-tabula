package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, GenerateExamGridSelectCourseCommand, GenerateExamGridSelectCourseCommandRequest, GenerateExamGridSelectCourseCommandState}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}

abstract class BaseCohortController extends BaseController
  with DepartmentScopedController with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent {

  type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState with SelfValidating

  val selectCourseAction: (Department, AcademicYear) => String
  val selectCourseActionLabel: String
  val selectCourseActionTitle: String

  protected def selectCourseRender(model: ModelMap, department: Department, academicYear: AcademicYear): String = {
    model.addAttribute("selectCourseAction", selectCourseAction(department, academicYear))
    model.addAttribute("selectCourseActionLabel", selectCourseActionLabel)
    model.addAttribute("selectCourseActionTitle", selectCourseActionTitle)
    "marks/admin/selectCourse"
  }

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

  @ModelAttribute("selectCourseCommand")
  def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): SelectCourseCommand =
    benchmarkTask("selectCourseCommand") {
      GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear), permitRoutesFromRootDepartment = securityService.can(user, departmentPermission, department.rootDepartment))
    }

}

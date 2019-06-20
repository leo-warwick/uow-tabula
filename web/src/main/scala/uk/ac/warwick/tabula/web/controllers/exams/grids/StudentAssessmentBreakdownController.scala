package uk.ac.warwick.tabula.web.controllers.exams.grids

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.{StudentAssessmentCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, NormalLoadLookup}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.{BreadCrumb, Mav, Routes, Breadcrumbs => BaseBreadcumbs}


@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/{studentCourseDetails}/assessmentdetails"))
class StudentAssessmentBreakdownController extends ExamsController
  with DepartmentScopedController with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
  with AutowiringCourseAndRouteServiceComponent with AutowiringModuleRegistrationServiceComponent with AutowiringNormalCATSLoadServiceComponent
  with TaskBenchmarking {


  type CommandType = Appliable[StudentMarksBreakdown] with StudentAssessmentCommandState

  override val departmentPermission: Permission = Permissions.Department.ExamGrids

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))


  @ModelAttribute("command")
  def command(@PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear): CommandType = {
    StudentAssessmentCommand(studentCourseDetails, mandatory(academicYear))
  }

  @ModelAttribute("weightings")
  def weightings(@PathVariable studentCourseDetails: StudentCourseDetails): IndexedSeq[CourseYearWeighting] = {
    (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
      courseAndRouteService.getCourseYearWeighting(mandatory(studentCourseDetails).course.code,
        mandatory(studentCourseDetails.sprStartAcademicYear), year)
    ).sorted
  }

  @RequestMapping()
  def viewStudentAssessmentDetails(
    @PathVariable studentCourseDetails: StudentCourseDetails,
    @PathVariable academicYear: AcademicYear,
    @ModelAttribute("command") cmd: CommandType
  ): Mav = {

    val breakdown = cmd.apply()
    val assessmentComponents = breakdown.modules
    val passMarkMap = assessmentComponents.map(ac => {
      val module = ac.moduleRegistration.module
      module -> ProgressionService.modulePassMark(module.degreeType)
    }).toMap
    val normalLoadLookup: NormalLoadLookup = NormalLoadLookup(academicYear, cmd.studentCourseYearDetails.yearOfStudy, normalCATSLoadService)

    Mav("exams/grids/generate/studentAssessmentComponentDetails",
      "passMarkMap" -> passMarkMap,
      "assessmentComponents" -> assessmentComponents,
      "normalLoadLookup" -> normalLoadLookup,
      "member" -> studentCourseDetails.student
    ).crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(mandatory(cmd.studentCourseYearDetails.enrolmentDepartment), mandatory(academicYear)))
      .secondCrumbs(secondBreadcrumbs(academicYear, studentCourseDetails)(scyd => Routes.exams.Grids.assessmentdetails(scyd)): _*)

  }

  def secondBreadcrumbs(activeAcademicYear: AcademicYear, scd: StudentCourseDetails)(urlGenerator: StudentCourseYearDetails => String): Seq[BreadCrumb] = {
    val chooseScyd = scd.freshStudentCourseYearDetailsForYear(activeAcademicYear) // fresh scyd for this year
      .orElse(scd.freshOrStaleStudentCourseYearDetailsForYear(activeAcademicYear))
      .getOrElse(throw new UnsupportedOperationException("Not valid StudentCourseYearDetails for given academic year"))

    val scyds = scd.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails) match {
      case Nil =>
        scd.student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
      case fresh =>
        fresh
    }
    scyds.map(scyd =>
      BaseBreadcumbs.Standard(
        title = "%s %s".format(scyd.studentCourseDetails.course.code, scyd.academicYear.getLabel),
        url = Some(urlGenerator(scyd)),
        tooltip = "%s %s".format(
          scyd.studentCourseDetails.course.name,
          scyd.academicYear.getLabel
        )
      ).setActive(scyd == chooseScyd)
    ).toSeq
  }
}

package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController


@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/preview/checkstudent"))
class StudentCheckController extends ExamsController {

  validatesSelf[SelfValidating]

  type SelectCourseCommand = Appliable[StudentCheckInfo] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
    ExamGridStudentCheckCommand(mandatory(department), mandatory(academicYear))

  @PostMapping
  def checkStudent(
    @Valid @ModelAttribute("command") command: SelectCourseCommand,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear
  ): Mav = if (errors.hasErrors) {
    Mav("exams/grids/generate/checkStudent", "errors" -> true).noLayout()
  } else {
    Mav("exams/grids/generate/checkStudent", "check" -> command.apply()).noLayout()
  }


}

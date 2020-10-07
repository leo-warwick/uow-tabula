package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, AssignmentToJsonConverter, AssignmentToXmlConverter}
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.XmlUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView, XmlErrorView, XmlView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

abstract class DepartmentAssignmentsController extends ApiController
  with AssignmentToJsonConverter
  with AssignmentToXmlConverter
  with AssessmentMembershipInfoToJsonConverter

@Controller
@RequestMapping(Array("/v1/department/{department}/assignments", "/v1/department/{department}/assignments.*"))
class ListAssignmentsForDepartmentController extends DepartmentAssignmentsController {

  @ModelAttribute("listCommand")
  def command(@PathVariable department: Department, user: CurrentUser): ViewViewableCommand[Department] =
    new ViewViewableCommand(Permissions.Module.ManageAssignments, mandatory(department))

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(@ModelAttribute("listCommand") command: ViewViewableCommand[Department], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      val department = command.apply()
      val assignments = department.modules.asScala.flatMap(_.assignments.asScala).filter { assignment =>
        !assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
      }

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "academicYear" -> Option(academicYear).map(_.toString).orNull,
        "assignments" -> assignments.map(jsonAssignmentObject(_))
      )))
    }
  }

  @RequestMapping(method = Array(GET), produces = Array("application/xml"))
  def listXML(@ModelAttribute("listCommand") command: ViewViewableCommand[Department], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
    if (errors.hasErrors) {
      Mav(new XmlErrorView(errors))
    } else {
      val department = command.apply()
      val assignments = department.modules.asScala.flatMap(_.assignments.asScala).filter { assignment =>
        !assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
      }

      Mav(new XmlView(
        <assignments>
          {assignments.map(xmlAssignmentObject)}
        </assignments> % Map(
          "success" -> true,
          "status" -> "ok",
          "academicYear" -> Option(academicYear).map(_.toString).orNull
        )
      ))
    }
  }
}

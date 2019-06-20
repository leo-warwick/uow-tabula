package uk.ac.warwick.tabula.web.controllers.attendance.manage

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.attendance.manage._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/{scheme}/edit/students"))
class EditStudentsOnSchemeController extends AbstractManageSchemeStudentsController {

  override protected val renderPath = "attendance/manage/editschemestudents"

  @RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.createAndAddPoints))
  def saveAndEditPoints(
    @Valid @ModelAttribute("persistanceCommand") cmd: Appliable[AttendanceMonitoringScheme],
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
    @PathVariable scheme: AttendanceMonitoringScheme
  ): Mav = {
    if (errors.hasErrors) {
      val findStudentsForSchemeCommandResult = findCommand.apply()
      val editMembershipCommandResult = editMembershipCommand.apply()
      render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
    } else {
      val scheme = cmd.apply()
      RedirectForce(Routes.Manage.editSchemePoints(scheme))
    }

  }

  @RequestMapping(method = Array(POST), params = Array(ManageSchemeMappingParameters.saveAndEditProperties))
  def saveAndEditProperties(
    @Valid @ModelAttribute("persistanceCommand") cmd: Appliable[AttendanceMonitoringScheme],
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: Appliable[FindStudentsForSchemeCommandResult] with FindStudentsForSchemeCommandState,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: Appliable[EditSchemeMembershipCommandResult],
    @PathVariable scheme: AttendanceMonitoringScheme
  ): Mav = {
    if (errors.hasErrors) {
      val findStudentsForSchemeCommandResult = findCommand.apply()
      val editMembershipCommandResult = editMembershipCommand.apply()
      render(scheme, findStudentsForSchemeCommandResult, editMembershipCommandResult)
    } else {
      val scheme = cmd.apply()
      RedirectForce(Routes.Manage.editScheme(scheme))
    }

  }
}
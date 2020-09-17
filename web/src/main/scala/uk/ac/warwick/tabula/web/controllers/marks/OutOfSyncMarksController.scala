package uk.ac.warwick.tabula.web.controllers.marks

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.{AcademicYear, SprCode}
import uk.ac.warwick.tabula.commands.{MemberOrUser, SelfValidating}
import uk.ac.warwick.tabula.commands.marks.{MarksManagementHomeCommand, OutOfSyncMarksCommand}
import uk.ac.warwick.tabula.data.model.{Department, Member}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringProfileServiceComponent, AutowiringUserLookupComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/marks/admin/{department}/{academicYear}/out-of-sync-marks"))
class OutOfSyncMarksController
  extends BaseController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringUserSettingsServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringMaintenanceModeServiceComponent
    with AutowiringProfileServiceComponent
    with AutowiringUserLookupComponent {

  validatesSelf[SelfValidating]

  override val departmentPermission: Permission = MarksManagementHomeCommand.AdminPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): OutOfSyncMarksCommand.Command =
    OutOfSyncMarksCommand(department, academicYear, user)

  private val formView: String = "marks/admin/out-of-sync-marks"

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Seq[BreadCrumb] = Seq(
    MarksBreadcrumbs.Admin.HomeForYear(department, academicYear),
    MarksBreadcrumbs.Admin.OutOfSyncMarks(department, academicYear, active = true)
  )

  @ModelAttribute("secondBreadcrumbs")
  def academicYearSwitcher(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Seq[BreadCrumb] =
    academicYearBreadcrumbs(academicYear)(Routes.marks.Admin.outOfSyncMarks(department, _))

  @ModelAttribute("membersByUniversityId")
  def membersByUniversityId(@ModelAttribute("command") command: OutOfSyncMarksCommand.Command): Map[String, MemberOrUser] = {
    val universityIds: Seq[String] = command.pendingComponentMarkChanges.flatMap(_._2).map(_.universityId).distinct
    val members: Map[String, Member] = profileService.getAllMembersWithUniversityIds(universityIds).map(m => m.universityId -> m).toMap
    val missingUniversityIds: Seq[String] = universityIds.filterNot(members.contains)

    if (missingUniversityIds.nonEmpty) {
      val users: Map[String, User] = userLookup.usersByWarwickUniIds(missingUniversityIds)

      members.view.mapValues(MemberOrUser(_)).toMap ++ users.view.mapValues(MemberOrUser(_)).toMap
    } else {
      members.view.mapValues(MemberOrUser(_)).toMap
    }
  }

  @ModelAttribute("membersBySprCode")
  def membersBySprCode(@ModelAttribute("command") command: OutOfSyncMarksCommand.Command): Map[String, MemberOrUser] =
    command.pendingModuleMarkChanges.flatMap(_._2).map(_.moduleRegistration).groupBy(_.sprCode).view.mapValues { regs =>
      val mr = regs.head
      MemberOrUser(Option(mr.studentCourseDetails).map(_.student), userLookup.getUserByWarwickUniId(SprCode.getUniversityId(mr.sprCode)))
    }.toMap

  @RequestMapping
  def form(@ModelAttribute("command") command: OutOfSyncMarksCommand.Command): String = {
    command.populate()
    formView
  }

  @PostMapping
  def sync(
    @Valid @ModelAttribute("command") command: OutOfSyncMarksCommand.Command,
    errors: Errors,
    @PathVariable department: Department,
    @PathVariable academicYear: AcademicYear,
    model: ModelMap,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      formView
    } else {
      command.apply()

      RedirectFlashing(Routes.marks.Admin.home(department, academicYear), "flash__success" -> "flash.outOfSyncMarks.accepted")
    }

}

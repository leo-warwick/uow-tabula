package uk.ac.warwick.tabula.commands.mitcircs

import org.hibernate.criterion.Order
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.{MitigatingCircumstancesSubmissionFilter, ScalaRestriction}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, AutowiringMitCircsWorkflowProgressServiceComponent, MitCircsSubmissionServiceComponent, MitCircsWorkflowProgressServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage, WorkflowStages}

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters._

case class MitigatingCircumstancesWorkflowProgress(percentage: Int, t: String, messageCode: String)

case class MitigatingCircumstancesSubmissionInfo(
  submission: MitigatingCircumstancesSubmission,
  progress: MitigatingCircumstancesWorkflowProgress,
  nextStage: Option[WorkflowStage],
  stages: ListMap[String, WorkflowStages.StageProgress],
)

case class AdminHomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmissionInfo],
)

object AdminHomeCommand {
  type Command = Appliable[AdminHomeInformation] with AdminHomeCommandRequest
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(department: Department, year: AcademicYear, user: CurrentUser): Command =
    new AdminHomeCommandInternal(department, year, user)
      with ComposableCommand[AdminHomeInformation]
      with AdminHomeCommandRequest
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringMitCircsWorkflowProgressServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringSecurityServiceComponent
      with AdminHomePermissions
      with ReadOnly with Unaudited
}

abstract class AdminHomeCommandInternal(val department: Department, val year: AcademicYear, val user: CurrentUser) extends CommandInternal[AdminHomeInformation]
  with AdminHomeCommandState {

  self: AdminHomeCommandRequest with MitCircsSubmissionServiceComponent with MitCircsWorkflowProgressServiceComponent =>

  override def applyInternal(): AdminHomeInformation =
    AdminHomeInformation(
      submissions = mitCircsSubmissionService.submissionsForDepartment(
        department,
        buildRestrictions(user, Seq(department), year, ScalaRestriction.is(
          "studentCourseYearDetails.academicYear", year,
          FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
        ).toSeq),
        MitigatingCircumstancesSubmissionFilter(
          affectedAssessmentModules = affectedAssessmentModules.asScala.toSet,
          includesStartDate = Option(includesStartDate),
          includesEndDate = Option(includesEndDate),
          approvedStartDate = Option(approvedStartDate),
          approvedEndDate = Option(approvedEndDate),
          state = state.asScala.toSet,
          isUnread = isUnread,
          // explicitly convert to scala boolean - otherwise null will always end up as Some(false) instead of None
          isCoronavirus = Option(isCoronavirus).map(_.booleanValue)
        )
      ).map { submission =>
        val progress = workflowProgressService.progress(department)(submission)

        MitigatingCircumstancesSubmissionInfo(
          submission = submission,
          progress = MitigatingCircumstancesWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,
        )
      },
    )
}

trait AdminHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AdminHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(AdminHomeCommand.RequiredPermission, mandatory(department))
  }
}

trait AdminHomeCommandState {

  self: SecurityServiceComponent =>

  // This is used to filter the department the MCS is made to
  def department: Department
  // This is used as part of filtering the students whose MCS should display
  def year: AcademicYear
  def user: CurrentUser
  def includeTier4Filters: Boolean = securityService.can(user, Profiles.Read.Tier4VisaRequirement, department)
}

trait AdminHomeCommandRequest extends FiltersStudents with AdminHomeCommandState {

  self : MitCircsSubmissionServiceComponent =>

  // This is for filtering the student who has made the submission
  var courseTypes: JList[CourseType] = JArrayList()
  var specificCourseTypes: JList[SpecificCourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var studyLevelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()

  // For filtering the submission itself
  var affectedAssessmentModules: JList[Module] = JArrayList()
  var includesStartDate: LocalDate = _
  var includesEndDate: LocalDate = _
  var approvedStartDate: LocalDate = _
  var approvedEndDate: LocalDate = _
  var state: JList[MitigatingCircumstancesSubmissionState] = JArrayList()
  var isUnread: Boolean = _
  var isCoronavirus: JBoolean = null

  lazy val possibleModules: Seq[Module] = {
    val affectedAssessmentModules = mitCircsSubmissionService.affectedModulesForDepartment(department).toSet -- allModules
    allModules ++ affectedAssessmentModules.toSeq.sortBy(_.code)
  }
  override val defaultOrder: Seq[Order] = Seq(Order.desc("_lastModified"))
  override val sortOrder: JList[Order] = JArrayList() // Not used
  override val modules: JList[Module] = JArrayList() // Not used
  override val hallsOfResidence: JList[String] = JArrayList() // Not used
  override val postcodes: JList[String] = JArrayList() // Not used
}

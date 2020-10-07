package uk.ac.warwick.tabula.commands.cm2

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.Module
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object CourseworkHomepageCommand {

  case class StudentAssignmentInformation(
    assignment: Assignment,
    submission: Option[Submission],
    extension: Option[Extension],
    extended: Boolean,
    hasActiveExtension: Boolean,
    extensionRequested: Boolean,
    studentDeadline: DateTime,
    submittable: Boolean,
    resubmittable: Boolean,
    feedback: Option[Feedback],
    feedbackDeadline: Option[LocalDate],
    feedbackLate: Boolean,
    lateFormative: Boolean
  )

  case class StudentAssignmentSummaryInformation(
    upcomingAssignments: Seq[StudentAssignmentInformation],
    actionRequiredAssignments: Seq[StudentAssignmentInformation],
    noActionRequiredAssignments: Seq[StudentAssignmentInformation],
    completedAssignments: Seq[StudentAssignmentInformation]
  ) {
    def isEmpty: Boolean = upcomingAssignments.isEmpty && actionRequiredAssignments.isEmpty && noActionRequiredAssignments.isEmpty && completedAssignments.isEmpty

    def nonempty: Boolean = !isEmpty

    def numLateFormative(infos: Seq[StudentAssignmentInformation]): Int = infos.count(_.lateFormative)
  }

  case class CourseworkHomepageAdminInformation(
    moduleManagerDepartments: Seq[Department],
    adminDepartments: Seq[Department]
  ) {
    def isEmpty: Boolean = moduleManagerDepartments.isEmpty && adminDepartments.isEmpty

    def nonempty: Boolean = !isEmpty
  }

  case class CourseworkHomepageInformation(
    homeDepartment: Option[Department],
    studentInformation: StudentAssignmentSummaryInformation,
    markingAcademicYears: Seq[AcademicYear],
    adminInformation: CourseworkHomepageAdminInformation
  )

  type Result = CourseworkHomepageInformation
  type Command = Appliable[Result] with CourseworkHomepageCommandState

  val AdminPermission: Module.ManageAssignments.type = Permissions.Module.ManageAssignments

  def apply(user: CurrentUser): Command =
    new CourseworkHomepageCommandInternal(user)
      with ComposableCommand[Result]
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringAssessmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with PubliclyVisiblePermissions with Unaudited with ReadOnly
}

trait CourseworkHomepageCommandState {
  def currentUser: CurrentUser
  lazy val user: User = currentUser.apparentUser
}

class CourseworkHomepageCommandInternal(val currentUser: CurrentUser) extends CommandInternal[Result]
  with CourseworkHomepageCommandState
  with CourseworkHomepageHomeDepartment
  with StudentAssignmentsSummary
  with MarkingSummaryCommandState
  with MarkingSummaryMarkerAssignmentList
  with CourseworkHomepageAdminDepartments
  with TaskBenchmarking {
  self: ModuleAndDepartmentServiceComponent
    with AssessmentServiceComponent
    with AssessmentMembershipServiceComponent
    with CM2MarkingWorkflowServiceComponent =>

  val target: MarkingSummaryCommandTarget = MarkingSummaryCurrentUserCommandTarget(currentUser, None)

  override def applyInternal(): Result =
    CourseworkHomepageInformation(
      homeDepartment,
      studentInformation,
      allCM2MarkerAssignments.map(_.academicYear).distinct.sorted,
      adminInformation
    )

}

trait CourseworkHomepageHomeDepartment extends TaskBenchmarking {
  self: CourseworkHomepageCommandState
    with ModuleAndDepartmentServiceComponent =>

  lazy val homeDepartment: Option[Department] = benchmarkTask("Get user's home department") {
    currentUser.departmentCode.maybeText.flatMap(moduleAndDepartmentService.getDepartmentByCode)
  }
}

trait CourseworkHomepageAdminDepartments extends TaskBenchmarking {
  self: CourseworkHomepageCommandState
    with ModuleAndDepartmentServiceComponent =>

  lazy val adminInformation: CourseworkHomepageAdminInformation = benchmarkTask("Get admin information") {
    CourseworkHomepageAdminInformation(
      moduleManagerDepartments,
      adminDepartments
    )
  }

  lazy val moduleManagerDepartments: Seq[Department] = benchmarkTask("Get module manager departments") {
    val ownedModules = benchmarkTask("Get owned modules") {
      moduleAndDepartmentService.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments)
    }

    ownedModules.map(_.adminDepartment).toSeq.sortBy(_.name)
  }

  lazy val adminDepartments: Seq[Department] = benchmarkTask("Get admin departments") {
    val ownedDepartments = benchmarkTask("Get owned departments") {
      moduleAndDepartmentService.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments)
    }

    ownedDepartments.toSeq.sortBy(_.name)
  }

}

trait StudentAssignmentsSummary extends TaskBenchmarking {
  self: AssessmentServiceComponent with AssessmentMembershipServiceComponent =>

  def user: User
  def academicYear: Option[AcademicYear]

  lazy val studentInformation: StudentAssignmentSummaryInformation = benchmarkTask("Get student information") {
    StudentAssignmentSummaryInformation(
      studentUpcomingAssignments,
      studentActionRequiredAssignments,
      studentNoActionRequiredAssignments,
      studentCompletedAssignments
    )
  }

  private lazy val assignmentsWithFeedback = benchmarkTask("Get assignments with feedback") {
    assessmentService.getAssignmentsWithFeedback(user.getUserId, academicYear).filter(_.publishFeedback) // Any academic year
  }

  private lazy val assignmentsWithSubmission = benchmarkTask("Get assignments with submission") {
    assessmentService.getAssignmentsWithSubmission(user.getUserId, academicYear) // Any academic year
  }

  private lazy val enrolledAssignments = benchmarkTask("Get enrolled assignments") {
    assessmentMembershipService.getEnrolledAssignments(user, academicYear) // Any academic year
  }

  private def lateFormative(assignment: Assignment) = !assignment.summative && assignment.isClosed

  // Public for testing
  def enhance(assignment: Assignment): StudentAssignmentInformation = {
    val extension = assignment.approvedExtensions.get(user.getUserId)
    // isExtended: is within an approved extension
    val isExtended = assignment.isWithinExtension(user)
    // hasActiveExtension: active = approved
    val hasActiveExtension = extension.nonEmpty
    val extensionRequested = assignment.allExtensions.get(user.getUserId).exists(_.exists(!_.isManual))
    val submission = assignment.submissions.asScala.find(_.isForUser(user))
    val feedback = assignment.feedbacks.asScala.filter(_.released).find(_.isForUser(user))
    val feedbackDeadline =
      if (assignment.collectSubmissions) submission.flatMap(assignment.feedbackDeadlineForSubmission)
      else assignment.feedbackDeadline.flatMap { wholeAssignmentDeadline =>
        // If we have an extension, use the extension's expiry date
        extension.filter(_.relevant).flatMap(_.feedbackDeadline).map(_.toLocalDate).orElse(Some(wholeAssignmentDeadline))
      }

    StudentAssignmentInformation(
      assignment = assignment,
      submission = submission,
      extension = extension,
      extended = isExtended,
      hasActiveExtension = hasActiveExtension,
      extensionRequested = extensionRequested,
      studentDeadline = assignment.submissionDeadline(user),
      submittable = assignment.submittable(user),
      resubmittable = assignment.resubmittable(user),
      feedback = feedback,
      feedbackDeadline = feedbackDeadline,
      feedbackLate = feedbackDeadline.exists(_.isBefore(LocalDate.now)),
      lateFormative = lateFormative(assignment)
    )
  }

  private def hasEarlierPersonalDeadline(ass1: Assignment, ass2: Assignment): Boolean = {
    // TAB-569 personal time to deadline - if ass1 is "due" before ass2 for the current user
    // Show open ended assignments after
    if (ass2.openEnded && !ass1.openEnded) true
    else if (ass1.openEnded && !ass2.openEnded) false
    else {
      def timeToDeadline(ass: Assignment) = {
        val extension = ass.approvedExtensions.get(user.getUserId)
        val isExtended = ass.isWithinExtension(user)

        if (ass.openEnded) ass.openDate
        else if (isExtended) extension.flatMap(_.expiryDate).getOrElse(ass.closeDate)
        else ass.closeDate
      }

      timeToDeadline(ass1) < timeToDeadline(ass2)
    }
  }

  private lazy val allUnsubmittedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get un-submitted assignments") {
    enrolledAssignments
      .diff(assignmentsWithFeedback)
      .diff(assignmentsWithSubmission)
      .sortWith(hasEarlierPersonalDeadline)
      .map(enhance)
  }

  private lazy val studentUpcomingAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get upcoming assignments") {
    allUnsubmittedAssignments
      .filter {
        case i if i.assignment.collectSubmissions => !i.assignment.isOpened && !i.lateFormative
        case _ => true
      }
  }

  private lazy val studentActionRequiredAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get action required assignments") {
    allUnsubmittedAssignments
      .diff(studentUpcomingAssignments)
      .filter(_.submittable)
  }

  private def hasEarlierEffectiveDate(ass1: StudentAssignmentInformation, ass2: StudentAssignmentInformation): Boolean = {
    def effectiveDate(info: StudentAssignmentInformation) =
      info.submission.map(_.submittedDate).getOrElse {
        val assignment = info.assignment
        if (assignment.openEnded) assignment.openDate
        else assignment.closeDate
      }

    effectiveDate(ass1) < effectiveDate(ass2)
  }

  lazy val studentNoActionRequiredAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get in-progress assignments") {
    val submittedAwaitingFeedback =
      assignmentsWithSubmission
        .diff(assignmentsWithFeedback)
        .filter(_.publishFeedback)
        .map(enhance)

    val unsubmittedAndUnsubmittable =
      allUnsubmittedAssignments.filterNot(_.lateFormative).filter(_.assignment.collectSubmissions)
        .diff(studentUpcomingAssignments)
        .diff(studentActionRequiredAssignments)
        .filterNot(_.lateFormative)

    (submittedAwaitingFeedback ++ unsubmittedAndUnsubmittable)
      .sortWith(hasEarlierEffectiveDate)
  }

  lazy val studentCompletedAssignments: Seq[StudentAssignmentInformation] = benchmarkTask("Get past assignments") {
    val lateFormativeAssignments = enrolledAssignments.filter(lateFormative).filterNot(_.publishFeedback)

    val submittedAwaitingFeedback = assignmentsWithSubmission.filterNot(lateFormative).filterNot(_.publishFeedback)

    (assignmentsWithFeedback ++ lateFormativeAssignments ++ submittedAwaitingFeedback)
      .map(enhance)
      .sortWith(hasEarlierEffectiveDate)
  }

}

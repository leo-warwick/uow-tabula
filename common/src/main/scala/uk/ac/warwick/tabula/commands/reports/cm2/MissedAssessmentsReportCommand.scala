package uk.ac.warwick.tabula.commands.reports.cm2

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandRequestValidation, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object MissedAssessmentsReportCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new MissedAssessmentsReportCommandInternal(department, academicYear)
      with ComposableCommand[MissedAssessmentsReport]
      with AutowiringAssessmentServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with ReportPermissions
      with ReportCommandRequestValidation
      with ReadOnly
      with Unaudited
}

class MissedAssessmentsReportCommandInternal(val department: Department, val academicYear: AcademicYear) extends CommandInternal[MissedAssessmentsReport] with ReportCommandRequest with ReportCommandState {
  self: AssessmentServiceComponent with ProfileServiceComponent with AssessmentMembershipServiceComponent =>

  override protected def applyInternal(): MissedAssessmentsReport = transactional(readOnly = true) {
    val assignments = assessmentService.getDepartmentAssignmentsClosingBetween(department, startDate, endDate)
      .filter(_.collectSubmissions)

    val assignmentUsers: Seq[(Assignment, User)] = assignments.flatMap(assignment => assessmentMembershipService.determineMembershipUsers(assignment).map(user => (assignment, user)))

    val members: Map[User, Option[Member]] = assignmentUsers.map(_._2).map(user => (user, profileService.getMemberByUser(user))).toMap

    val entities = assignmentUsers.flatMap { case (assignment, user) =>
      members.get(user).flatMap { case Some(student) =>
        val submission = assignment.submissions.asScala.find(_.isForUser(user))
        val workingDaysLateIfSubmittedNow = assignment.workingDaysLateIfSubmittedNow(user.getUserId)

        if (submission.forall(_.isLate) && workingDaysLateIfSubmittedNow > 0) {
          val extension = assignment.approvedExtensions.values.find(_.isForUser(user))

          Some(MissedAssessmentsReportEntity(
            student = student,
            module = assignment.module,
            assignment = assignment,
            submission = submission,
            extension = extension,
            workingDaysLate = submission.map(assignment.workingDaysLate).getOrElse(workingDaysLateIfSubmittedNow)
          ))
        } else None
      }
    }

    MissedAssessmentsReport(entities)
  }
}

case class MissedAssessmentsReport(entities: Seq[MissedAssessmentsReportEntity])

case class MissedAssessmentsReportEntity(
  student: Member,
  module: Module,
  assignment: Assignment,
  submission: Option[Submission],
  extension: Option[Extension],
  workingDaysLate: Int
)


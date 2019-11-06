package uk.ac.warwick.tabula.commands.cm2

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FeedbackChangeNotification, FeedbackPublishedNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object StudentSubmissionAndFeedbackCommand {

  case class StudentSubmissionInformation(
    submission: Option[Submission],
    feedback: Option[Feedback],
    extension: Option[Extension],
    isExtended: Boolean,
    extensionRequested: Boolean,
    canSubmit: Boolean,
    canReSubmit: Boolean,
    disability: Option[Disability]
  )

  def apply(assignment: Assignment, member: Member, viewingUser: CurrentUser) =
    new StudentMemberSubmissionAndFeedbackCommandInternal(assignment, member, viewingUser)
      with StudentMemberSubmissionAndFeedbackCommandPermissions
      with AutowiringFeedbackServiceComponent
      with AutowiringSubmissionServiceComponent
      with AutowiringProfileServiceComponent
      with ComposableCommand[StudentSubmissionInformation]
      with Unaudited with ReadOnly

  def apply(assignment: Assignment, user: CurrentUser) =
    new CurrentUserSubmissionAndFeedbackCommandInternal(assignment, user)
      with CurrentUserSubmissionAndFeedbackCommandPermissions
      with CurrentUserSubmissionAndFeedbackNotificationCompletion
      with AutowiringFeedbackServiceComponent
      with AutowiringSubmissionServiceComponent
      with AutowiringProfileServiceComponent
      with ComposableCommand[StudentSubmissionInformation]
      with Unaudited with ReadOnly
}

trait StudentSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent =>

  def assignment: Assignment

  def studentUser: User

  def viewer: User

  lazy val feedback: Option[AssignmentFeedback] =
    feedbackService.getAssignmentFeedbackByUsercode(assignment, studentUser.getUserId).filter(_.released)
  lazy val submission: Option[Submission] =
    submissionService.getSubmissionByUsercode(assignment, studentUser.getUserId).filter(_.submitted)
}

trait StudentMemberSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent =>

  def studentMember: Member

  def currentUser: CurrentUser

  final lazy val studentUser: User = studentMember.asSsoUser
  final lazy val viewer: User = currentUser.apparentUser
}

trait CurrentUserSubmissionAndFeedbackCommandState extends StudentSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent =>

  def currentUser: CurrentUser

  final lazy val studentUser: User = currentUser.apparentUser
  final lazy val viewer: User = currentUser.apparentUser
}

abstract class StudentMemberSubmissionAndFeedbackCommandInternal(assignment: Assignment, val studentMember: Member, val currentUser: CurrentUser)
  extends StudentSubmissionAndFeedbackCommandInternal(assignment) with StudentMemberSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent with ProfileServiceComponent =>
}

abstract class CurrentUserSubmissionAndFeedbackCommandInternal(assignment: Assignment, val currentUser: CurrentUser)
  extends StudentSubmissionAndFeedbackCommandInternal(assignment) with CurrentUserSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent with ProfileServiceComponent =>
}

abstract class StudentSubmissionAndFeedbackCommandInternal(val assignment: Assignment)
  extends CommandInternal[StudentSubmissionInformation] with StudentSubmissionAndFeedbackCommandState {
  self: FeedbackServiceComponent with SubmissionServiceComponent with ProfileServiceComponent =>

  def applyInternal(): StudentSubmissionInformation = {
    // Log a ViewOnlineFeedback event if the student itself is viewing
    feedback.filter(_.usercode == viewer.getUserId).foreach { feedback =>
      ViewOnlineFeedbackCommand(feedback).apply()
    }

    StudentSubmissionInformation(
      submission = submission,
      feedback = HibernateHelpers.initialiseAndUnproxy(feedback),
      extension = assignment.approvedExtensions.get(studentUser.getUserId),

      isExtended = assignment.isWithinExtension(studentUser),
      extensionRequested = assignment.allExtensions.get(studentUser.getUserId).exists(_.exists(!_.isManual)),

      canSubmit = assignment.submittable(studentUser),
      canReSubmit = assignment.resubmittable(studentUser),

      disability = profileService.getMemberByUser(studentUser).flatMap {
        case student: StudentMember => student.disability.filter(_.reportable)
        case _ => None
      }
    )
  }

}

trait StudentMemberSubmissionAndFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentMemberSubmissionAndFeedbackCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Submission.Read, mandatory(studentMember))
    p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(studentMember))
  }
}

trait CurrentUserSubmissionAndFeedbackCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CurrentUserSubmissionAndFeedbackCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    var perms = collection.mutable.ListBuffer[CheckablePermission]()

    submission.foreach { submission => perms += CheckablePermission(Permissions.Submission.Read, Some(submission)) }
    feedback.foreach { feedback => perms += CheckablePermission(Permissions.AssignmentFeedback.Read, Some(feedback)) }

    perms += CheckablePermission(Permissions.Submission.Create, Some(assignment))

    p.PermissionCheckAny(perms)
  }
}

trait CurrentUserSubmissionAndFeedbackNotificationCompletion extends CompletesNotifications[StudentSubmissionInformation] {

  self: NotificationHandling with StudentSubmissionAndFeedbackCommandState =>

  def notificationsToComplete(commandResult: StudentSubmissionInformation): CompletesNotificationsResult = {
    commandResult.feedback match {
      case Some(feedbackResult: AssignmentFeedback) =>
        CompletesNotificationsResult(
          notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackPublishedNotification](feedbackResult) ++
            notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackChangeNotification](feedbackResult),
          viewer
        )
      case _ =>
        EmptyCompletesNotificationsResult
    }
  }

}

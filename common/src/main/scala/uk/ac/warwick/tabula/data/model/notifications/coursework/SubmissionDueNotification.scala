package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import org.joda.time.{DateTime, Days}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AutowiringUserLookupComponent}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.util.Try

trait SubmissionReminder extends RecipientCompletedActionRequiredNotification {
  self: Notification[_, Unit]
    with NotificationPreSaveBehaviour =>

  def deadline: Option[DateTime]

  def assignment: Assignment

  def module: Module = assignment.module

  def moduleCode: String = module.code.toUpperCase

  def referenceDate: DateTime = created

  def cantSubmit: Boolean = !assignment.allowLateSubmissions && deadline.exists(DateTime.now.isAfter)

  def daysLeft: Int = deadline.map(_.withTimeAtStartOfDay()).map { closeDate =>
    val now = referenceDate.withTimeAtStartOfDay()
    Days.daysBetween(now, closeDate).getDays
  }.getOrElse(0)

  override final def onPreSave(newRecord: Boolean): Unit =
    priority = Try {
      if (daysLeft == 1) {
        Warning
      } else if (daysLeft < 1) {
        Critical
      } else {
        Info
      }
    }.getOrElse(Info) // deadline could be null in which case we won't be sending anything so Info is fine

  def url: String = Routes.cm2.assignment(assignment)

  def urlTitle = "upload your submission"

  def title = s"$moduleCode: Your submission for '${assignment.name}' $timeStatement"

  def timeStatement: String = if (daysLeft > 1) {
    s"is due in $daysLeft days"
  } else if (daysLeft == 1) {
    "is due tomorrow"
  } else if (daysLeft == 0) {
    "is due today"
  } else if (daysLeft == -1) {
    "is 1 day late"
  } else {
    s"is ${0 - daysLeft} days late"
  }

  def be: String = if (daysLeft >= 0) "is" else "was"

  def deadlineDate: String = be + " " + deadline.map(dateTimeFormatter.print).getOrElse("[unknown]")

  def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/emails/submission_reminder.ftl", Map(
    "assignment" -> assignment,
    "module" -> module,
    "timeStatement" -> timeStatement,
    "cantSubmit" -> cantSubmit,
    "deadlineDate" -> deadlineDate
  ))

  def verb = "Remind"

  def shouldSend: Boolean = assignment.collectSubmissions && !assignment.openEnded && assignment.isVisibleToStudents
}

object SubmissionReminderBatchedNotificationHandler extends BatchedNotificationHandler[Notification[_, Unit] with SubmissionReminder] {

  private def daysLeftTimeStatementForBatch(daysLeft: Int, batchSize: Int): String =
    if (batchSize > 1) {
      if (daysLeft > 1) {
        s"Your submissions for $batchSize assignments are due in $daysLeft days"
      } else if (daysLeft == 1) {
        s"Your submissions for $batchSize assignments are due tomorrow"
      } else if (daysLeft == 0) {
        s"Your submissions for $batchSize assignments are due today"
      } else if (daysLeft == -1) {
        s"Your submissions for $batchSize assignments are 1 day late"
      } else {
        s"Your submissions for $batchSize assignments are ${0 - daysLeft} days late"
      }
    } else {
      if (daysLeft > 1) {
        s"Your submission for 1 assignment is due in $daysLeft days"
      } else if (daysLeft == 1) {
        s"Your submission for 1 assignment is due tomorrow"
      } else if (daysLeft == 0) {
        s"Your submission for 1 assignment is due today"
      } else if (daysLeft == -1) {
        s"Your submission for 1 assignment is 1 day late"
      } else {
        s"Your submission for 1 assignment is ${0 - daysLeft} days late"
      }
    }

  private def groupedByDaysLeft(batch: Seq[Notification[_, Unit] with SubmissionReminder]): Seq[(Int, Seq[Notification[_, Unit] with SubmissionReminder])] =
    batch.groupBy(_.daysLeft)
      .view
      .mapValues(_.sortBy(n => (n.daysLeft, n.deadline, n.moduleCode, n.assignment.name)))
      .toSeq
      .sortBy { case (daysLeft, _) => daysLeft }

  override def titleForBatchInternal(notifications: Seq[Notification[_, Unit] with SubmissionReminder], user: User): String = {
    val batches = groupedByDaysLeft(notifications)

    val (daysLeftMostUrgent, mostUrgent) = batches.head
    val otherAssignments = batches.tail.flatMap { case (_, batch) => batch }

    val mostUrgentTitle =
      if (mostUrgent.size == 1) mostUrgent.head.titleFor(user)
      else daysLeftTimeStatementForBatch(daysLeftMostUrgent, mostUrgent.size)

    val othersTitle =
      if (otherAssignments.isEmpty) ""
      else if (otherAssignments.size == 1) " (+ 1 other)"
      else s" (+ ${otherAssignments.size} others)"

    s"$mostUrgentTitle$othersTitle"
  }

  override def contentForBatchInternal(notifications: Seq[Notification[_, Unit] with SubmissionReminder]): FreemarkerModel = {
    val batches = groupedByDaysLeft(notifications)

    FreemarkerModel("/WEB-INF/freemarker/emails/submission_reminder_batch.ftl", Map(
      "batches" -> batches.map { case (daysLeft, batch) =>
        Map(
          "timeStatement" -> daysLeftTimeStatementForBatch(daysLeft, batch.size),
          "items" -> batch.map(_.content.model)
        )
      }
    ))
  }

  override def urlForBatchInternal(notifications: Seq[Notification[_, Unit] with SubmissionReminder], user: User): String =
    Routes.cm2.home

  override def urlTitleForBatchInternal(notifications: Seq[Notification[_, Unit] with SubmissionReminder]): String =
    "view your assignments"

}

@Entity
@Proxy
@DiscriminatorValue("SubmissionDueGeneral")
class SubmissionDueGeneralNotification
  extends BatchedNotification[Assignment, Unit, Notification[_, Unit] with SubmissionReminder](SubmissionReminderBatchedNotificationHandler)
    with SingleItemNotification[Assignment]
    with SubmissionReminder {

  @transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

  def deadline: Option[DateTime] = Option(assignment.closeDate)

  def assignment: Assignment = item.entity

  def recipients: Seq[User] =
    if (!shouldSend)
      Nil
    else {
      val submissions = assignment.submissions.asScala
      val extensions = assignment.approvedExtensions // TAB-2303
      val allStudents = membershipService.determineMembershipUsers(assignment)
      // first filter out students that have submitted already
      val withoutSubmission = allStudents.filterNot(user => submissions.exists(_.isForUser(user)))
      // finally filter students that have an approved extension
      withoutSubmission.filterNot(user => extensions.contains(user.getUserId))
    }
}

@Entity
@Proxy
@DiscriminatorValue("SubmissionDueExtension")
class SubmissionDueWithExtensionNotification
  extends BatchedNotification[Extension, Unit, Notification[_, Unit] with SubmissionReminder](SubmissionReminderBatchedNotificationHandler)
    with SingleItemNotification[Extension]
    with SubmissionReminder
    with AutowiringUserLookupComponent {

  @transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

  def extension: Extension = item.entity

  def deadline: Option[DateTime] = extension.expiryDate

  def assignment: Assignment = extension.assignment

  def recipients: Seq[User] =
    if (!shouldSend)
      Nil
    else {
      val hasSubmitted = assignment.submissions.asScala.exists(_.usercode == extension.usercode)

      // Get the latest extended deadline if a student has multiple.
      lazy val isTheLatestApprovedExtension = assignment.approvedExtensions.get(extension.usercode).contains(extension)

      lazy val isAMemberOfTheAssignment = membershipService.determineMembershipUsers(assignment).exists(_.getUserId == extension.usercode)

      // Don't send if the user has submitted or if there's no expiry date on the extension (i.e. it's been rejected)
      // or if there is an extension with a later extended deadline for this user or if the extension deadline is earlier than the assignment's close date
      // or if the student is no longer a member of the assignment
      // or if the student's deadline is in the past, and this assignment doesn't allow submission after the deadline
      if (hasSubmitted || !extension.approved || extension.expiryDate.isEmpty ||
        !shouldSend || !isTheLatestApprovedExtension || !extension.relevant || !isAMemberOfTheAssignment || cantSubmit) {
        Nil
      } else {
        Seq(userLookup.getUserByUserId(extension.usercode))
      }
    }

}

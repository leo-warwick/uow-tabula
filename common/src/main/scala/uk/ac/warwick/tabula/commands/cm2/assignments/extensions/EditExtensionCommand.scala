package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.data.model.triggers.{ExtensionExpiredTrigger, Trigger}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Notification, ScheduledNotification, ToEntityReference}
import uk.ac.warwick.tabula.events.{NotificationHandling, TriggerHandling}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.userlookup.User

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object EditExtensionCommand {
  def apply(assignment: Assignment, student: User, currentUser: CurrentUser) =
    new EditExtensionCommandInternal(assignment, student, currentUser)
      with ComposableCommand[Extension]
      with EditExtensionCommandPermissions
      with EditExtensionCommandDescription
      with EditExtensionCommandValidation
      with EditExtensionCommandNotification
      with EditExtensionCommandScheduledNotification
      with EditExtensionCommandNotificationCompletion
      with ExtensionTriggers
      with AutowiringUserLookupComponent
      with HibernateExtensionPersistenceComponent
}

class EditExtensionCommandInternal(val assignment: Assignment, val student: User, val submitter: CurrentUser) extends CommandInternal[Extension]
  with EditExtensionCommandState with EditExtensionCommandValidation with BindListener {

  self: ExtensionPersistenceComponent with UserLookupComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }

  val e: Option[Extension] = assignment.requestedOrApprovedExtensions.get(student.getUserId)
  e match {
    case Some(ext) =>
      copyFrom(ext)
      extension = ext
      isNew = false
    case None =>
      extension = new Extension
      extension.usercode = student.getUserId
      extension._universityId = student.getWarwickId
      isNew = true
  }

  def applyInternal(): Extension = transactional() {
    copyTo(extension)
    save(extension)
    extension
  }

  def copyTo(extension: Extension): Unit = {
    extension.usercode = student.getUserId
    extension._universityId = student.getWarwickId
    extension.assignment = assignment
    extension.expiryDate = expiryDate
    extension.updateState(state, reviewerComments)

    if (extension.attachments != null) {
      // delete attachments that have been removed
      val matchingAttachments: mutable.Set[FileAttachment] = extension.attachments.asScala -- attachedFiles.asScala
      matchingAttachments.foreach(delete)
    }

    for (attachment <- file.attached.asScala) {
      extension.addAttachment(attachment)
    }
  }

  def copyFrom(extension: Extension): Unit = {
    expiryDate = extension.expiryDate.orNull
    state = extension.state
    reviewerComments = extension.reviewerComments
    attachedFiles = JHashSet(extension.attachments)
  }

}

trait EditExtensionCommandState {

  var isNew: Boolean = _

  def student: User

  def assignment: Assignment

  def submitter: CurrentUser

  @WithinYears(maxFuture = 3)
  @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
  var expiryDate: DateTime = _
  var reviewerComments: String = _
  var state: ExtensionState = ExtensionState.Unreviewed

  var file: UploadedFile = new UploadedFile
  var attachedFiles: JSet[FileAttachment] = JSet()

  var extension: Extension = _

}

trait EditExtensionCommandValidation extends SelfValidating {
  self: EditExtensionCommandState =>
  def validate(errors: Errors): Unit = {
    if (state == ExtensionState.Unreviewed) {
      errors.rejectValue("state", "extension.state.empty")
    }
    if (expiryDate == null) {
      if (state == ExtensionState.Approved) {
        errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
      }
    } else if (expiryDate.isBefore(assignment.closeDate)) {
      errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
    }

    if (!Option(reviewerComments).exists(_.nonEmpty) && state == ExtensionState.MoreInformationRequired) {
      errors.rejectValue("reviewerComments", "extension.reviewerComments.provideReasons")
    }
  }
}

trait EditExtensionCommandPermissions extends RequiresPermissionsChecking {
  self: EditExtensionCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    extension match {
      case e if e.isTransient => p.PermissionCheck(Permissions.Extension.Create, assignment)
      case _ => p.PermissionCheck(Permissions.Extension.Update, assignment)
    }
  }
}


trait EditExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
  self: EditExtensionCommandState =>

  def emit(extension: Extension): Seq[ExtensionNotification] = {
    val admin = submitter.apparentUser

    if (extension.isManual) {
      if (isNew) {
        Seq(Notification.init(new ExtensionGrantedNotification, submitter.apparentUser, Seq(extension), assignment))
      } else {
        Seq(Notification.init(new ExtensionChangedNotification, submitter.apparentUser, Seq(extension), assignment))
      }
    } else {
      val (baseNotification, baseAdminNotification) = if (extension.approved) {
        (new ExtensionRequestApprovedNotification, new ExtensionRequestRespondedApproveNotification)
      } else if (extension.moreInfoRequired) {
        (new ExtensionRequestMoreInfo, new ExtensionRequestRespondedMoreInfoNotification)
      } else {
        (new ExtensionRequestRejectedNotification, new ExtensionRequestRespondedRejectNotification)
      }
      Seq(
        Notification.init(baseNotification, admin, Seq(extension), assignment),
        Notification.init(baseAdminNotification, admin, Seq(extension), assignment)
      )
    }
  }
}

trait EditExtensionCommandScheduledNotification extends SchedulesNotifications[Extension, Extension] {
  self: EditExtensionCommandState =>

  override def transformResult(extension: Extension) = Seq(extension)

  override def scheduledNotifications(extension: Extension): Seq[ScheduledNotification[Extension]] = {
    val notifications = for (
      extension <- Seq(extension) if extension.isManual || extension.approved;
      expiryDate <- extension.expiryDate
    ) yield {

      val assignment = extension.assignment
      val dayOfDeadline = Assignment.onTheDayReminderDateTime(expiryDate)

      val submissionNotifications = {
        // skip the week late notification if late submission isn't possible
        val daysToSend = if (assignment.allowLateSubmissions) {
          Seq(-7, -1, 1, 7)
        } else {
          Seq(-7, -1, 1)
        }
        val surroundingTimes = for (day <- daysToSend) yield expiryDate.plusDays(day)
        val proposedTimes = Seq(dayOfDeadline) ++ surroundingTimes
        // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
        val allTimes = proposedTimes.filter(_.isAfterNow)

        allTimes.map {
          when =>
            new ScheduledNotification[Extension]("SubmissionDueExtension", extension, when)
        }
      }

      val feedbackNotifications =
        if (assignment.dissertation || !assignment.publishFeedback)
          Seq()
        else {
          val daysToSend = Seq(-7, -1, 0)
          val proposedTimes = for (day <- daysToSend; fd <- extension.feedbackDeadline) yield fd.plusDays(day)

          // Filter out all times that are in the past. This should only generate ScheduledNotifications for the future.
          val allTimes = proposedTimes.filter(_.isAfterNow)

          allTimes.map {
            when =>
              new ScheduledNotification[Extension]("FeedbackDueExtension", extension, when)
          }
        }

      submissionNotifications ++ feedbackNotifications
    }
    notifications.flatten
  }

}

trait EditExtensionCommandNotificationCompletion extends CompletesNotifications[Extension] {

  self: NotificationHandling with EditExtensionCommandState =>

  def notificationsToComplete(commandResult: Extension): CompletesNotificationsResult = {
    if (commandResult.approved || commandResult.rejected) {
      val created = notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestCreatedNotification](commandResult)
      val modified = notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestModifiedNotification](commandResult)
      val info = notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionInfoReceivedNotification](commandResult)
      CompletesNotificationsResult(created ++ modified ++ info, submitter.apparentUser)
    } else {
      EmptyCompletesNotificationsResult
    }
  }
}

trait EditExtensionCommandDescription extends Describable[Extension] {
  self: EditExtensionCommandState =>

  override lazy val eventName: String = "EditExtension"

  def describe(d: Description): Unit = {
    if (isNew) {
      d.assignment(assignment)
       .studentIds(Option(student.getWarwickId).toSeq)
       .studentUsercodes(student.getUserId)
       .extensionState(state)
    } else d.extension(extension)

    d.fileAttachments(attachedFiles.asScala.toSeq)
  }
}

trait ExtensionTriggers extends GeneratesTriggers[Extension] {
  self: TriggerHandling =>

  override def generateTriggers(extension: Extension): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
    if (extension.approved) {
      extension.expiryDate.toSeq.map { expiryDate =>
        ExtensionExpiredTrigger(expiryDate, extension)
      }
    } else {
      Seq.empty
    }
  }
}

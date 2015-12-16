package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.commands.{CompletesNotifications, SchedulesNotifications, Notifies, ComposableCommand}
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{ScheduledNotification, Assignment, Module, Notification}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.Transactions._

object EditExtensionCommand {
	def apply(module: Module, assignment: Assignment, uniId: String, currentUser: CurrentUser, action: String) =
		new EditExtensionCommandInternal(module, assignment, uniId, currentUser, action)
			with ComposableCommand[Extension]
			with EditExtensionCommandPermissions
			with ModifyExtensionCommandDescription
			with ModifyExtensionCommandValidation
			with EditExtensionCommandNotification
			with EditExtensionCommandScheduledNotification
			with EditExtensionCommandNotificationCompletion
			with AutowiringUserLookupComponent
			with AutowiringExtensionServiceComponent
}

class EditExtensionCommandInternal(module: Module, assignment: Assignment, uniId: String, currentUser: CurrentUser, action: String)
		extends ModifyExtensionCommand(module, assignment, uniId, currentUser, action) with ModifyExtensionCommandState  {
	self: ExtensionServiceComponent with UserLookupComponent =>

	val e = assignment.findExtension(universityId)
	e match {
		case Some(ext) =>
			copyFrom(ext)
			extension = ext
			isNew = false
		case None =>
			extension = new Extension(universityId)
			isNew = true
	}

	def applyInternal() = transactional() {
		copyTo(extension)
		extensionService.save(extension)
		extension
	}
}


trait EditExtensionCommandPermissions extends RequiresPermissionsChecking {
	self: ModifyExtensionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)

		extension match {
			case e if e.isTransient => p.PermissionCheck(Permissions.Extension.Create, assignment)
			case _ => p.PermissionCheck(Permissions.Extension.Update, assignment)
		}
	}
}


trait EditExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: ModifyExtensionCommandState =>

	def emit(extension: Extension) = {
		val admin = submitter.apparentUser

		if (extension.isManual) {
			if(isNew) {
				Seq(Notification.init(new ExtensionGrantedNotification, submitter.apparentUser, Seq(extension), assignment))
			} else {
				Seq(Notification.init(new ExtensionChangedNotification, submitter.apparentUser, Seq(extension), assignment))
			}
		} else {
			val baseNotification = if (extension.approved) {
				new ExtensionRequestApprovedNotification
			} else {
				new ExtensionRequestRejectedNotification
			}
			val studentNotification = Notification.init(baseNotification, admin, Seq(extension), assignment)

			val baseAdminNotification = if (extension.approved) {
				new ExtensionRequestRespondedApproveNotification
			} else {
				new ExtensionRequestRespondedRejectNotification
			}
			val adminNotifications = Notification.init(baseAdminNotification, admin, Seq(extension), assignment)

			Seq(studentNotification, adminNotifications)
		}
	}
}

trait EditExtensionCommandScheduledNotification extends SchedulesNotifications[Extension, Extension] {
	self: ModifyExtensionCommandState =>

	override def transformResult(extension: Extension) = Seq(extension)

	override def scheduledNotifications(extension: Extension) = {
		val notifications = for (
			extension <- Seq(extension) if extension.isManual || extension.approved;
			expiryDate <- extension.expiryDate
		) yield {

			val assignment = extension.assignment
			val dayOfDeadline = expiryDate.withTime(0, 0, 0, 0)

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
				if (assignment.dissertation)
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

	self: NotificationHandling with ModifyExtensionCommandState =>

	def notificationsToComplete(commandResult: Extension): CompletesNotificationsResult = {
		if (commandResult.approved || commandResult.rejected)
			CompletesNotificationsResult(
				notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestCreatedNotification](commandResult) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ExtensionRequestModifiedNotification](commandResult)
				,
				submitter.apparentUser
			)
		else
			EmptyCompletesNotificationsResult
	}
}

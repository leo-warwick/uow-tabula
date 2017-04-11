package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Notification, ScheduledNotification}
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears


object ModifyExtensionCommand {
	def apply(extension: Extension, submitter: CurrentUser) = new ModifyExtensionCommandInternal(extension, submitter)
		with ComposableCommand[Extension]
		with ModifyExtensionValidation
		with ModifyExtensionPermissions
		with ModifyExtensionDescription
		with ModifyExtensionNotification
		with ModifyExtensionScheduledNotification
		with ModifyExtensionNotificationCompletion
		with HibernateExtensionPersistenceComponent
}

class ModifyExtensionCommandInternal(val extension: Extension, val submitter: CurrentUser) extends CommandInternal[Extension]
	with ModifyExtensionState with ModifyExtensionValidation  with TaskBenchmarking {

	this: ExtensionPersistenceComponent  =>

	expiryDate = extension.expiryDate.orNull
	reviewerComments = extension.reviewerComments
	state = extension.state

	def copyTo(extension: Extension): Unit = {
		extension.expiryDate = expiryDate
		extension.updateState(state, reviewerComments)
	}

	def applyInternal(): Extension = transactional() {
		copyTo(extension)
		save(extension)
		extension
	}
}

trait ModifyExtensionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyExtensionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Extension.Update, extension)
	}
}

trait ModifyExtensionState {
	val extension: Extension
	val submitter: CurrentUser

	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var expiryDate: DateTime = _
	var reviewerComments: String = _
	var state: ExtensionState = _
}

trait ModifyExtensionValidation extends SelfValidating {
	self: ModifyExtensionState =>
	def validate(errors: Errors) {
		if(expiryDate == null) {
			if (state == ExtensionState.Approved) {
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(expiryDate.isBefore(extension.assignment.closeDate)) {
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}

		if(!Option(reviewerComments).exists(_.nonEmpty) && state == ExtensionState.MoreInformationRequired) {
			errors.rejectValue("reviewerComments", "extension.reviewerComments.provideReasons")
		}
	}
}

trait ModifyExtensionDescription extends Describable[Extension] {
	self: ModifyExtensionState =>

	def describe(d: Description) {
		d.assignment(extension.assignment)
		d.module(extension.assignment.module)
		d.studentIds(extension.universityId.toSeq)
		d.studentUsercodes(extension.usercode)
	}
}

trait ModifyExtensionNotification extends Notifies[Extension, Option[Extension]] {
	self: ModifyExtensionState =>

	def emit(extension: Extension): Seq[ExtensionNotification] = {
		val admin = submitter.apparentUser

		if (extension.isManual) {
			Seq(Notification.init(new ExtensionChangedNotification, admin, Seq(extension), extension.assignment))
		} else {
			val (baseNotification, baseAdminNotification) = if (extension.approved) {
				(new ExtensionRequestApprovedNotification, new ExtensionRequestRespondedApproveNotification)
			} else if (extension.moreInfoRequired){
				(new ExtensionRequestMoreInfo, new ExtensionRequestRespondedMoreInfoNotification)
			} else {
				(new ExtensionRequestRejectedNotification, new ExtensionRequestRespondedRejectNotification)
			}
			Seq(
				Notification.init(baseNotification, admin, Seq(extension), extension.assignment),
				Notification.init(baseAdminNotification, admin, Seq(extension), extension.assignment)
			)
		}
	}
}

trait ModifyExtensionScheduledNotification extends SchedulesNotifications[Extension, Extension] {
	self: ModifyExtensionState =>

	override def transformResult(extension: Extension) = Seq(extension)

	override def scheduledNotifications(extension: Extension): Seq[ScheduledNotification[Extension]] = {
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

trait ModifyExtensionNotificationCompletion extends CompletesNotifications[Extension] {

	self: NotificationHandling with ModifyExtensionState =>

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
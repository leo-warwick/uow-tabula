package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification, ScheduledNotification}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.userlookup.User

object EditExtensionCommand {
	def apply(module: Module, assignment: Assignment, student: User, currentUser: CurrentUser, action: String) =
		new EditExtensionCommandInternal(module, assignment, student, currentUser, action)
			with ComposableCommand[Extension]
			with EditExtensionCommandPermissions
			with EditExtensionCommandDescription
			with EditExtensionCommandValidation
			with EditExtensionCommandNotification
			with EditExtensionCommandScheduledNotification
			with EditExtensionCommandNotificationCompletion
			with AutowiringUserLookupComponent
			with HibernateExtensionPersistenceComponent
}

class EditExtensionCommandInternal(val module: Module, val assignment: Assignment, val student: User, val submitter: CurrentUser, val action: String) extends CommandInternal[Extension]
		with EditExtensionCommandState with EditExtensionCommandValidation with TaskBenchmarking {

	self: ExtensionPersistenceComponent with UserLookupComponent =>

	val e: Option[Extension] = assignment.findExtension(student.getUserId)
	e match {
		case Some(ext) =>
			copyFrom(ext)
			extension = ext
			isNew = false
		case None =>
			extension = new Extension
			extension.usercode = student.getUserId
			extension._universityId = student.getUserId
			isNew = true
	}

	def applyInternal(): Extension = transactional() {
		copyTo(extension)
		save(extension)
		extension
	}

	def copyTo(extension: Extension): Unit = {
		extension._universityId = student.getUserId
		extension.assignment = assignment
		extension.expiryDate = expiryDate
		extension.rawState_=(state)
		extension.reviewedOn = DateTime.now

		action match {
			case ApprovalAction | UpdateApprovalAction => extension.approve(reviewerComments)
			case RejectionAction => extension.reject(reviewerComments)
			case RevocationAction => extension.rawState_=(ExtensionState.Revoked)
			case _ =>
		}
	}

	def copyFrom(extension: Extension): Unit = {
		expiryDate = extension.expiryDate.orNull
		state = extension.state
		reviewerComments = extension.reviewerComments
	}

}

trait EditExtensionCommandState {

	var isNew: Boolean = _

	def student: User
	def assignment: Assignment
	def module: Module
	def submitter: CurrentUser
	def action: String

	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var expiryDate: DateTime =_
	var reviewerComments: String =_
	var state: ExtensionState = ExtensionState.Unreviewed

	var extension: Extension =_

	final val ApprovalAction = "Grant"
	final val RejectionAction = "Reject"
	final val RevocationAction = "Revoke"
	final val UpdateApprovalAction = "Update"

}

trait EditExtensionCommandValidation extends SelfValidating {
	self: EditExtensionCommandState =>
	def validate(errors: Errors) {
		if(expiryDate == null) {
			if (state == ExtensionState.Approved) {
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(expiryDate.isBefore(assignment.closeDate)) {
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}

		if(!Option(reviewerComments).exists(_.nonEmpty) && state == ExtensionState.MoreInformationRequired) {
			errors.rejectValue("reviewerComments", "extension.reviewerComments.provideReasons")
		}
	}
}

trait EditExtensionCommandPermissions extends RequiresPermissionsChecking {
	self: EditExtensionCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)

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
	self: EditExtensionCommandState =>

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

trait EditExtensionCommandNotificationCompletion extends CompletesNotifications[Extension] {

	self: NotificationHandling with EditExtensionCommandState =>

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

trait EditExtensionCommandDescription extends Describable[Extension] {
	self: EditExtensionCommandState =>

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		d.studentIds(Option(student.getWarwickId).toSeq)
		d.studentUsercodes(student.getUserId)
	}
}
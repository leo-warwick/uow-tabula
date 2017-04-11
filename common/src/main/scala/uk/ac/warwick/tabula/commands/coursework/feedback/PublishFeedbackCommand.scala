package uk.ac.warwick.tabula.commands.coursework.feedback

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FinaliseFeedbackNotification, FeedbackDueGeneralNotification, FeedbackDueExtensionNotification, FeedbackPublishedNotification}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions._
import language.implicitConversions
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.commands.coursework.feedback.PublishFeedbackCommand.PublishFeedbackResults

object PublishFeedbackCommand {
	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)

	case class PublishFeedbackResults(
		notifications: Seq[Notification[AssignmentFeedback, Assignment]] = Nil,
		missingUsers: Seq[MissingUser] = Nil,
		badEmails: Seq[BadEmail] = Nil
	)

def apply(module: Module, assignment: Assignment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
	new PublishFeedbackCommandInternal(module, assignment, submitter, gradeGenerator)
		with ComposableCommand[PublishFeedbackCommand.PublishFeedbackResults]
		with AutowiringFeedbackServiceComponent
		with AutowiringFeedbackForSitsServiceComponent
		with PublishFeedbackCommandState
		with PublishFeedbackPermissions
		with PublishFeedbackValidation
		with PublishFeedbackDescription
		with PublishFeedbackNotification
		with PublishFeedbackNotificationCompletion
		with QueuesFeedbackForSits
}

class PublishFeedbackCommandInternal(val module: Module, val assignment: Assignment, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[PublishFeedbackCommand.PublishFeedbackResults]{

	self: PublishFeedbackCommandState with QueuesFeedbackForSits =>

	import PublishFeedbackCommand._

	def applyInternal(): PublishFeedbackResults = {

		val allResults = feedbackToRelease.map { case(usercode, user, feedback) =>
			feedback.released = true
			feedback.releasedDate = new DateTime
			if (sendToSits)	queueFeedback(feedback, submitter, gradeGenerator)
			generateNotification(usercode, user, feedback)
		}

		allResults.foldLeft(PublishFeedbackResults()) { (acc, result) =>
				PublishFeedbackResults(
					notifications = acc.notifications ++ result.notifications,
					missingUsers = acc.missingUsers ++ result.missingUsers,
					badEmails = acc.badEmails ++ result.badEmails
				)
			}
	}

	private def generateNotification(usercode: String, user: User, feedback: Feedback) = {
		feedback match {
			case assignmentFeedback: AssignmentFeedback =>
				if (user.isFoundUser) {
					val email = user.getEmail
					if (email.hasText) {
						val n = Notification.init(new FeedbackPublishedNotification, submitter.apparentUser, Seq(assignmentFeedback), assignmentFeedback.assignment)
						n.recipientUniversityId = user.getUserId
						PublishFeedbackResults(
							notifications = Seq(n)
						)
					} else {
						PublishFeedbackResults(
							badEmails = Seq(PublishFeedbackCommand.BadEmail(user))
						)
					}
				} else {
					PublishFeedbackResults(
						missingUsers = Seq(PublishFeedbackCommand.MissingUser(usercode))
					)
				}
			case _ => PublishFeedbackResults()
		}
	}

}

trait PublishFeedbackNotification extends Notifies[PublishFeedbackCommand.PublishFeedbackResults, Feedback] {
	override def emit(results: PublishFeedbackResults): Seq[Notification[AssignmentFeedback, Assignment]] = results.notifications
}

trait PublishFeedbackNotificationCompletion extends CompletesNotifications[PublishFeedbackCommand.PublishFeedbackResults] {
	self: PublishFeedbackCommandState with NotificationHandling =>

	def notificationsToComplete(commandResult: PublishFeedbackCommand.PublishFeedbackResults): CompletesNotificationsResult = {
		val feedbackNotifications = commandResult.notifications.flatMap(_.entities).flatMap(feedback =>
			notificationService.findActionRequiredNotificationsByEntityAndType[FinaliseFeedbackNotification](feedback)
		)
		if (!assignment.needsFeedbackPublishing) {
			CompletesNotificationsResult(
				feedbackNotifications ++
					notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueGeneralNotification](assignment) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[FeedbackDueExtensionNotification](assignment),
				submitter.apparentUser
			)
		} else {
			CompletesNotificationsResult(feedbackNotifications, submitter.apparentUser)
		}
	}
}

trait PublishFeedbackCommandState {

	self: FeedbackServiceComponent with FeedbackForSitsServiceComponent =>

	val module: Module
	val assignment: Assignment
	val submitter: CurrentUser
	val gradeGenerator: GeneratesGradesFromMarks

	var confirm: Boolean = false
	var sendToSits: Boolean = false

	lazy val feedbackToRelease: Seq[(String, User, Feedback)] = for {
		(usercode, user) <- feedbackService.getUsersForFeedback(assignment)
		feedback <- assignment.fullFeedback.find(_.usercode == usercode)
	} yield (usercode, user, feedback)


	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed) {
			errors.reject("feedback.publish.notclosed")
		} else if (assignment.fullFeedback.isEmpty) {
			errors.reject("feedback.publish.nofeedback")
		}
	}

	def validateGrades: ValidateAndPopulateFeedbackResult = {
		feedbackForSitsService.validateAndPopulateFeedback(feedbackToRelease.map(_._3), gradeGenerator)
	}
}

trait QueuesFeedbackForSits extends FeedbackForSitsServiceComponent {

	def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits] =
		feedbackForSitsService.queueFeedback(feedback, submitter, gradeGenerator)
}

trait PublishFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: PublishFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.AssignmentFeedback.Publish, assignment)
	}
}

trait PublishFeedbackValidation extends SelfValidating {

	self: PublishFeedbackCommandState =>

	override def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) {
			errors.rejectValue("confirm", "feedback.publish.confirm")
		}
	}
}

trait PublishFeedbackDescription extends Describable[PublishFeedbackCommand.PublishFeedbackResults] {

	self: PublishFeedbackCommandState with FeedbackServiceComponent =>

	override def describe(d: Description) {
		val students = feedbackToRelease map { case(_, user, _) => user }
		d.assignment(assignment)
		 .studentIds(students.flatMap(m => Option(m.getWarwickId)))
		 .studentUsercodes(students.map(_.getUserId))
	}
}
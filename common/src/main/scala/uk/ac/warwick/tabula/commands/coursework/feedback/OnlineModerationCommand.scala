package uk.ac.warwick.tabula.commands.coursework.feedback

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.{FinaliseFeedbackComponent, FinaliseFeedbackComponentImpl}
import uk.ac.warwick.tabula.data.AutowiringSavedFormValueDaoComponent
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ModeratorRejectedNotification, ReleaseToMarkerNotification, ReturnToMarkerNotification}
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Notification, _}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object OnlineModerationCommand {
	def apply(module: Module, assignment: Assignment, student: User, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new OnlineModerationCommand(module, assignment, student, marker, submitter, gradeGenerator)
			with ComposableCommand[MarkerFeedback]
			with OnlineFeedbackFormPermissions
			with MarkerFeedbackStateCopy
			with CopyFromFormFields
			with WriteToFormFields
			with ModerationRejectedNotifier
			with OnlineModerationNotificationCompletion
			with FinaliseFeedbackComponentImpl
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with OnlineFeedbackFormDescription[MarkerFeedback] {
				override lazy val eventName = "OnlineMarkerFeedback"
			}
}

abstract class OnlineModerationCommand(
	module: Module,
	assignment: Assignment,
	student: User,
	val user: User,
	val submitter: CurrentUser,
	gradeGenerator: GeneratesGradesFromMarks
) extends AbstractOnlineFeedbackFormCommand(module, assignment, student, user, gradeGenerator) with CommandInternal[MarkerFeedback] with Appliable[MarkerFeedback]
	with ModerationState with UserAware {

	self: FeedbackServiceComponent with FileAttachmentServiceComponent with ZipServiceComponent with MarkerFeedbackStateCopy
		with FinaliseFeedbackComponent =>

	def markerFeedback: Option[MarkerFeedback] = assignment.getMarkerFeedback(student.getUserId, marker, SecondFeedback)

	copyState(markerFeedback, copyModerationFieldsFrom)

	def applyInternal(): MarkerFeedback = {

		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.asScala.find(_.usercode == student.getUserId).getOrElse({
			val newFeedback = new AssignmentFeedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = marker.getUserId
			newFeedback._universityId = student.getWarwickId
			newFeedback.usercode = student.getUserId
			newFeedback.released = false
			newFeedback.createdDate = DateTime.now
			newFeedback
		})

		val firstMarkerFeedback = parentFeedback.getFirstMarkerFeedback.getOrElse(throw new IllegalArgumentException("Could not find first marker feedback"))
		secondMarkerFeedback = parentFeedback.getSecondMarkerFeedback.getOrElse {
			val mf = new MarkerFeedback(parentFeedback)
			parentFeedback.secondMarkerFeedback = mf
			mf
		}
		val markerFeedback = Seq(firstMarkerFeedback, secondMarkerFeedback)

		// if the second-marker feedback is already rejected then do nothing - UI should prevent this
		if(secondMarkerFeedback.state != Rejected){
			if (approved) {
				finaliseFeedback(assignment, Seq(firstMarkerFeedback))
				secondMarkerFeedback.state = MarkingCompleted
			} else {
				markerFeedback.foreach(_.state = Rejected)
				copyModerationFieldsTo(secondMarkerFeedback)
			}
			parentFeedback.updatedDate = DateTime.now
			feedbackService.saveOrUpdate(parentFeedback)
			markerFeedback.foreach(feedbackService.save)
		}
		firstMarkerFeedback
	}

	def copyModerationFieldsFrom(markerFeedback: MarkerFeedback) {

		copyFrom(markerFeedback)

		if(markerFeedback.rejectionComments.hasText) {
			rejectionComments = markerFeedback.rejectionComments
		}

		approved = markerFeedback.state != Rejected
	}

	def copyModerationFieldsTo(markerFeedback: MarkerFeedback) {

		copyTo(markerFeedback)

		if(rejectionComments.hasText) {
			markerFeedback.rejectionComments = rejectionComments
		}
	}

	override def validate(errors: Errors) {
		super.fieldValidation(errors)

		if (Option(approved).isEmpty)
			errors.rejectValue("approved", "markers.moderation.approved.notDefined")
		else if(!approved && !rejectionComments.hasText)
			errors.rejectValue("rejectionComments", "markers.moderation.rejectionComments.empty")


	}

}

trait ModerationState {
	var approved: Boolean = true
	var secondMarkerFeedback: MarkerFeedback = _
}

trait ModerationRejectedNotifier extends Notifies[MarkerFeedback, MarkerFeedback] {
	self: ModerationState with UserAware with UserLookupComponent with Logging =>

	def emit(rejectedFeedback: MarkerFeedback): Seq[Notification[MarkerFeedback, Unit]] = approved match {
		case false => Seq (
			Notification.init(new ModeratorRejectedNotification, user, Seq(secondMarkerFeedback))
		)
		case true => Nil
	}
}

trait OnlineModerationNotificationCompletion extends CompletesNotifications[MarkerFeedback] {
	self: ModerationState with UserAware with NotificationHandling =>

	def notificationsToComplete(commandResult: MarkerFeedback): CompletesNotificationsResult = {
		CompletesNotificationsResult(
			notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](secondMarkerFeedback) ++
				notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](secondMarkerFeedback),
			user
		)
	}
}

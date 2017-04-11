package uk.ac.warwick.tabula.commands.coursework.assignments

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.{FeedbackReleasedNotifier, ReleasedState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ModeratorRejectedNotification, ReleaseToMarkerNotification, ReturnToMarkerNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

object MarkingCompletedCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser) =
		new MarkingCompletedCommand(module, assignment, marker, submitter)
			with ComposableCommand[Unit]
			with MarkingCompletedCommandPermissions
			with MarkingCompletedDescription
			with SecondMarkerReleaseNotifier
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
			with MarkerCompletedNotificationCompletion
			with FinaliseFeedbackComponentImpl
}

abstract class MarkingCompletedCommand(val module: Module, val assignment: Assignment, val user: User, val submitter: CurrentUser)
	extends CommandInternal[Unit] with SelfValidating with UserAware with MarkingCompletedState with ReleasedState with BindListener with CreatesNextMarkerFeedback with CanProxy {

	self: StateServiceComponent with FeedbackServiceComponent with FinaliseFeedbackComponent =>

	override def onBind(result: BindingResult) {
		// filter out any feedbacks where the current user is not the marker
		markerFeedback = markerFeedback.asScala.filter(_.getMarkerUser.exists { _ == user }).asJava

		// Pre-submit validation
		noMarks = markerFeedback.asScala.filter(!_.hasMark)
		noFeedback = markerFeedback.asScala.filter(!_.hasFeedback)
		releasedFeedback = markerFeedback.asScala.filter(_.state == MarkingState.MarkingCompleted)
	}

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("markerFeedback", "markerFeedback.finishMarking.noStudents")
	}

	override def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = markerFeedback.asScala -- releasedFeedback

		feedbackForRelease.foreach(stateService.updateState(_, MarkingState.MarkingCompleted))

		releaseNextMarkerFeedbackOrFinalise(feedbackForRelease)
	}

	private def releaseNextMarkerFeedbackOrFinalise(feedbackForRelease: mutable.Buffer[MarkerFeedback]) {
		newReleasedFeedback = feedbackForRelease.flatMap(createNextMarkerFeedback).map { nextMarkerFeedback =>
			stateService.updateState(nextMarkerFeedback, MarkingState.ReleasedForMarking)
			feedbackService.save(nextMarkerFeedback)
			nextMarkerFeedback
		}.asJava

		val feedbackToFinalise = feedbackForRelease.filter(mf => mf.getFeedbackPosition match {
			case FirstFeedback => !mf.feedback.markingWorkflow.hasSecondMarker
			case SecondFeedback => !mf.feedback.markingWorkflow.hasThirdMarker
			case _ => true
		})
		if (feedbackToFinalise.nonEmpty)
			finaliseFeedback(assignment, feedbackToFinalise)
	}
}

trait CreatesNextMarkerFeedback {
	def createNextMarkerFeedback(markerFeedback: MarkerFeedback): Option[MarkerFeedback] = {
		markerFeedback.getFeedbackPosition match {
			case FirstFeedback if markerFeedback.feedback.markingWorkflow.hasSecondMarker =>
				val mf = new MarkerFeedback(markerFeedback.feedback)
				markerFeedback.feedback.secondMarkerFeedback = mf
				Some(mf)
			case SecondFeedback if markerFeedback.feedback.markingWorkflow.hasThirdMarker =>
				val mf = new MarkerFeedback(markerFeedback.feedback)
				markerFeedback.feedback.thirdMarkerFeedback = mf
				Some(mf)
			case _ => None
		}
	}
}

trait MarkingCompletedCommandPermissions extends RequiresPermissionsChecking {
	self: MarkingCompletedState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkingCompletedDescription extends Describable[Unit] {

	self: MarkingCompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> markerFeedback.asScala.map(_.feedback.usercode))
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> markerFeedback.size())
	}
}

trait MarkingCompletedState {
	self : UserAware =>

	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var markerFeedback: JList[MarkerFeedback] = JArrayList()

	var noMarks: Seq[MarkerFeedback] = Nil
	var noFeedback: Seq[MarkerFeedback] = Nil
	var releasedFeedback: Seq[MarkerFeedback] = Nil

	var onlineMarking: Boolean = false
	var confirm: Boolean = false
	val marker: User = user
	val submitter: CurrentUser
}

trait SecondMarkerReleaseNotifier extends FeedbackReleasedNotifier[Unit] {
	self: MarkingCompletedState with ReleasedState with UserAware with UserLookupComponent with Logging =>
	def blankNotification = new ReleaseToMarkerNotification(2)
}

trait MarkerCompletedNotificationCompletion extends CompletesNotifications[Unit] {

	self: MarkingCompletedState with NotificationHandling with FeedbackServiceComponent =>

	def notificationsToComplete(commandResult: Unit): CompletesNotificationsResult = {
		val notificationsToComplete = markerFeedback.asScala
			.filter(_.state == MarkingState.MarkingCompleted)
			.flatMap(mf =>
				// TAB-4328-  ModeratorRejectedNotification is orphaned at this stage.
				rejectedMarkerFeedbacks(mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
			)
		CompletesNotificationsResult(notificationsToComplete, marker)
	}


	private def rejectedMarkerFeedbacks(mf: MarkerFeedback): Seq[ActionRequiredNotification] = {
		feedbackService.getRejectedMarkerFeedbackByFeedback(mf.feedback).flatMap(rejectedMarkerFeedback =>
			notificationService.findActionRequiredNotificationsByEntityAndType[ModeratorRejectedNotification](rejectedMarkerFeedback)
		)
	}
}


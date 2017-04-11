package uk.ac.warwick.tabula.commands.coursework.assignments

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipService, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

/**
 * Copies the appropriate MarkerFeedback item to its parent Feedback ready for processing by administrators
 */
object FinaliseFeedbackCommand {
	def apply(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback], user: User) =
		new FinaliseFeedbackCommandInternal(assignment, markerFeedbacks, user)
			with ComposableCommand[Seq[Feedback]]
			with FinaliseFeedbackPermissions
			with FinaliseFeedbackDescription
			with FinaliseFeedbackNotifier
}

trait FinaliseFeedbackCommandState {
	def assignment: Assignment
	def markerFeedbacks: Seq[MarkerFeedback]
}

abstract class FinaliseFeedbackCommandInternal(val assignment: Assignment, val markerFeedbacks: Seq[MarkerFeedback], val user: User)
	extends CommandInternal[Seq[Feedback]] with FinaliseFeedbackCommandState with UserAware {

	var fileDao: FileDao = Wire.auto[FileDao]
	var zipService: ZipService = Wire.auto[ZipService]

	override def applyInternal(): Seq[Feedback] = {
		markerFeedbacks.map { markerFeedback =>
			this.copyToFeedback(markerFeedback)
		}
	}

	private def copyToFeedback(markerFeedback: MarkerFeedback): Feedback = {
		val parent = markerFeedback.feedback

		parent.clearCustomFormValues()

		// save custom fields
		parent.customFormValues.addAll(markerFeedback.customFormValues.map { formValue =>
			val newValue = new SavedFormValue()
			newValue.name = formValue.name
			newValue.feedback = formValue.markerFeedback.feedback
			newValue.value = formValue.value
			newValue
		}.toSet[SavedFormValue])

		parent.actualGrade = markerFeedback.grade
		parent.actualMark = markerFeedback.mark

		parent.updatedDate = DateTime.now

		// erase any existing attachments - these will be replaced
		parent.clearAttachments()

		markerFeedback.attachments.foreach(parent.addAttachment)
		zipService.invalidateIndividualFeedbackZip(parent)
		parent
	}
}

trait FinaliseFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FinaliseFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, mandatory(assignment))
	}
}

trait FinaliseFeedbackDescription extends Describable[Seq[Feedback]] {
	self: FinaliseFeedbackCommandState =>

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("updatedFeedback" -> markerFeedbacks.size)
	}

}

trait FinaliseFeedbackNotifier extends Notifies[Seq[Feedback], Seq[Feedback]] {
	self: FinaliseFeedbackCommandState with UserAware =>

	override def emit(feedbacks: Seq[Feedback]): Seq[Notification[Feedback, Assignment]] = {
		Seq(Notification.init(new FinaliseFeedbackNotification, user, feedbacks.filterNot { _.checkedReleased }, assignment))
	}
}

trait FinaliseFeedbackComponent {
	def finaliseFeedback(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback])
}

trait FinaliseFeedbackComponentImpl extends FinaliseFeedbackComponent {
	self: UserAware =>

	def finaliseFeedback(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback]) {
		val finaliseFeedbackCommand = FinaliseFeedbackCommand(assignment, markerFeedbacks, user)
		finaliseFeedbackCommand.apply()
	}
}
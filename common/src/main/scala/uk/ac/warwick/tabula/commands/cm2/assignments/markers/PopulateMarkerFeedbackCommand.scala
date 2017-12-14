package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

/**
 * Copies the previous MarkerFeedback item to these marker feedbacks
 */
object PopulateMarkerFeedbackCommand {
	def apply(assignment: Assignment, markerFeedback: Seq[MarkerFeedback]) =
		new PopulateMarkerFeedbackCommandInternal(assignment, markerFeedback)
			with ComposableCommand[Seq[MarkerFeedback]]
			with PopulateMarkerFeedbackPermissions
			with PopulateMarkerFeedbackDescription
			with AutowiringFeedbackServiceComponent
}

trait PopulateMarkerFeedbackCommandState {
	def assignment: Assignment
	def markerFeedback: Seq[MarkerFeedback]
}

abstract class PopulateMarkerFeedbackCommandInternal(val assignment: Assignment, val markerFeedback: Seq[MarkerFeedback])
	extends CommandInternal[Seq[MarkerFeedback]] with PopulateMarkerFeedbackCommandState {

	this: FeedbackServiceComponent =>

	override def applyInternal(): Seq[MarkerFeedback] = {
		markerFeedback.foreach(mf => {
			val allMarkerFeedback = mf.feedback.allMarkerFeedback
			val previousStage = mf.stage.previousStages.headOption
			for(ps <- previousStage; pmf <- allMarkerFeedback.find(_.stage == ps)) copyPreviousFeedback(pmf, mf)
		})
		markerFeedback
	}

	private def copyPreviousFeedback(previous: MarkerFeedback, markerFeedback: MarkerFeedback): MarkerFeedback = {
		markerFeedback.clearCustomFormValues()

		val previousFormValues = previous.customFormValues.asScala.map { formValue =>
			val newValue = new SavedFormValue()
			newValue.name = formValue.name
			newValue.markerFeedback = markerFeedback
			newValue.value = formValue.value
			newValue
		}.toSet[SavedFormValue]

		// save custom fields
		markerFeedback.customFormValues.addAll(previousFormValues.asJava)

		markerFeedback.grade = previous.grade
		markerFeedback.mark = previous.mark

		// erase any existing attachments - these will be replaced
		markerFeedback.clearAttachments()
		val newAttachments = previous.attachments.asScala.map(fa => {
			val newAttachment = fa.duplicate()
			newAttachment.markerFeedback = markerFeedback
			newAttachment
		})
		newAttachments.foreach(markerFeedback.addAttachment)
		feedbackService.save(markerFeedback)
		markerFeedback
	}
}

trait PopulateMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: PopulateMarkerFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, mandatory(assignment))
	}
}

trait PopulateMarkerFeedbackDescription extends Describable[Seq[MarkerFeedback]] {
	self: PopulateMarkerFeedbackCommandState =>

	override lazy val eventName: String = "PopulateMarkerFeedback"

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("copiedFeedback" -> markerFeedback.size)
	}

}

trait PopulateMarkerFeedbackComponent {
	def populateMarkerFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback])
}

trait PopulateMarkerFeedbackComponentImpl extends PopulateMarkerFeedbackComponent {

	def populateMarkerFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback]) {
		val populateMarkerFeedbackCommand = PopulateMarkerFeedbackCommand(assignment, markerFeedback)
		populateMarkerFeedbackCommand.apply()
	}
}
package uk.ac.warwick.tabula.commands.coursework

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assessment, Feedback, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

object UploadFeedbackToSitsCommand {
	def apply(module: Module, assessment: Assessment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new UploadFeedbackToSitsCommandInternal(module, assessment, currentUser, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringFeedbackForSitsServiceComponent
			with ComposableCommand[Seq[Feedback]]
			with UploadFeedbackToSitsDescription
			with UploadFeedbackToSitsPermissions
			with UploadFeedbackToSitsCommandState
			with UploadFeedbackToSitsCommandRequest
}


class UploadFeedbackToSitsCommandInternal(val module: Module, val assessment: Assessment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Seq[Feedback]] {

	self: FeedbackServiceComponent with FeedbackForSitsServiceComponent with UploadFeedbackToSitsCommandState =>

	lazy val gradeValidation: ValidateAndPopulateFeedbackResult = feedbackForSitsService.validateAndPopulateFeedback(feedbacks, gradeGenerator)

	override def applyInternal(): Seq[Feedback] = {
		feedbacks.flatMap(f => feedbackForSitsService.queueFeedback(f, currentUser, gradeGenerator)).map(_.feedback)
	}

}

trait UploadFeedbackToSitsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UploadFeedbackToSitsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assessment), mandatory(module))
		p.PermissionCheck(Permissions.AssignmentFeedback.Publish, assessment)
	}

}

trait UploadFeedbackToSitsDescription extends Describable[Seq[Feedback]] {

	self: UploadFeedbackToSitsCommandState =>

	override lazy val eventName = "UploadFeedbackToSits"

	override def describe(d: Description) {
		d.assessment(assessment)
	}

	override def describeResult(d: Description, result: Seq[Feedback]): Unit = {
		d.property("students" -> result.map(_.usercode))
	}
}

trait UploadFeedbackToSitsCommandState {

	self: UploadFeedbackToSitsCommandRequest =>

	def module: Module
	def assessment: Assessment
	lazy val feedbacks: Seq[Feedback] = assessment.fullFeedback.filter(f => students.isEmpty || students.asScala.contains(f.usercode))
}

trait UploadFeedbackToSitsCommandRequest {
	var students: JList[String] = JArrayList()
}

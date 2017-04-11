package uk.ac.warwick.tabula.commands.exams.exams

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.exams.ExamMarkedNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.coursework.docconversion.AutowiringMarksExtractorComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ExamMarkerAddMarksCommand {
	def apply(module: Module, assessment: Assessment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new AdminAddMarksCommandInternal(module, assessment, submitter, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringMarksExtractorComponent
			with ComposableCommand[Seq[Feedback]]
			with MarkerAddMarksDescription
			with MarkerAddMarksPermissions
			with AdminAddMarksCommandValidation
			with MarkerAddMarksNotifications
			with AdminAddMarksCommandState
			with PostExtractValidation
			with AddMarksCommandBindListener
}

trait MarkerAddMarksDescription extends Describable[Seq[Feedback]] {

	self: AdminAddMarksCommandState =>

	override lazy val eventName = "MarkerAddMarks"

	override def describe(d: Description) {
		assessment match {
			case assignment: Assignment => d.assignment(assignment)
			case exam: Exam => d.exam(exam)
		}

	}
}

trait MarkerAddMarksNotifications extends Notifies[Seq[Feedback], Feedback] {

	self: AdminAddMarksCommandState =>

	def emit(updatedFeedback: Seq[Feedback]): Seq[ExamMarkedNotification] = updatedFeedback.headOption.flatMap { feedback => HibernateHelpers.initialiseAndUnproxy(feedback) match {
		case examFeedback: ExamFeedback =>
			Option(Notification.init(new ExamMarkedNotification, submitter.apparentUser, examFeedback, examFeedback.exam))
			case _ => None
	}}.map(Seq(_)).getOrElse(Seq())

}

trait MarkerAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AdminAddMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		p.PermissionCheck(Permissions.ExamMarkerFeedback.Manage, assessment)
	}

}
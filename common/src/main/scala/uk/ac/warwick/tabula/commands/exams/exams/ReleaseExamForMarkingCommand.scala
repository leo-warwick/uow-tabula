package uk.ac.warwick.tabula.commands.exams.exams

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.exams.ExamReleasedForMarkingNotification
import uk.ac.warwick.tabula.data.model.{Exam, Module, Notification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

object ReleaseExamForMarkingCommand {
	def apply(module: Module, exam: Exam, currentUser: CurrentUser) =
		new ReleaseExamForMarkingCommandInternal(module, exam, currentUser)
			with ComposableCommand[Exam]
			with AutowiringAssessmentServiceComponent
			with ReleaseExamForMarkingCommandDescription
			with ReleaseExamForMarkingCommandPermissions
			with ReleaseExamForMarkingCommandState
			with ExamReleasedNotifier
}


class ReleaseExamForMarkingCommandInternal(val module: Module, val exam: Exam, currentUser: CurrentUser)
	extends CommandInternal[Exam] {

	self: AssessmentServiceComponent =>

	val user: User = currentUser.apparentUser

	override def applyInternal(): Exam = {
		exam.released = true
		assessmentService.save(exam)
		exam
	}

}

trait ReleaseExamForMarkingCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReleaseExamForMarkingCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(exam), mandatory(module))
		p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
	}

}

trait ReleaseExamForMarkingCommandDescription extends Describable[Exam] {

	self: ReleaseExamForMarkingCommandState =>

	override lazy val eventName = "ReleaseExamForMarking"

	override def describe(d: Description) {
		d.exam(exam)
	}
}

trait ReleaseExamForMarkingCommandState {
	def module: Module
	def exam: Exam
	def user: User
}

trait ExamReleasedNotifier extends Notifies[Exam, Exam] {

	self : ReleaseExamForMarkingCommandState =>

	def emit(result: Exam): mutable.Buffer[ExamReleasedForMarkingNotification] = {
		val notifications = exam.firstMarkers.asScala
			.filterNot(map => map.students.isEmpty || map.marker_id == null) // don't notify markers with no assigned students
			.map(map => {
				val notification = Notification.init(new ExamReleasedForMarkingNotification, user, exam)
				notification.recipientUserId = map.marker_id
				notification
			})

		notifications
	}
}

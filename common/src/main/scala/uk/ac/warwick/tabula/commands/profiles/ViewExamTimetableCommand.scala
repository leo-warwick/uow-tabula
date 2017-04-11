package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables.ExamTimetableFetchingService.ExamTimetable
import uk.ac.warwick.tabula.services.timetables.{AutowiringExamTimetableFetchingServiceComponent, ExamTimetableFetchingService, ExamTimetableFetchingServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object ViewExamTimetableCommand {

	val Timeout: FiniteDuration = 15.seconds

	def apply(member: Member, viewer: CurrentUser) =
		new ViewExamTimetableCommandInternal(member, viewer)
			with AutowiringExamTimetableFetchingServiceComponent
			with ComposableCommand[Try[ExamTimetableFetchingService.ExamTimetable]]
			with ViewExamTimetablePermissions
			with ViewExamTimetableCommandState
			with ReadOnly with Unaudited
}


class ViewExamTimetableCommandInternal(val member: Member, viewer: CurrentUser) extends CommandInternal[Try[ExamTimetableFetchingService.ExamTimetable]] {

	self: ExamTimetableFetchingServiceComponent =>

	override def applyInternal(): Try[ExamTimetable] = {
		Try(Await.result(
			examTimetableFetchingService.getTimetable(member, viewer), ViewExamTimetableCommand.Timeout
		))
	}

}

trait ViewExamTimetablePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewExamTimetableCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Timetable, member)
	}

}

trait ViewExamTimetableCommandState {
	def member: Member
}

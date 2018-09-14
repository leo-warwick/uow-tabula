package uk.ac.warwick.tabula.commands.cm2.assignments

import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionListItem
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global

object ListSubmissionsCommand {
	type CommandType = Appliable[Future[Seq[SubmissionListItem]]] with ListSubmissionsRequest

	def apply(assignment: Assignment) =
		new ListSubmissionsCommandInternal(assignment)
			with ComposableCommand[Future[Seq[SubmissionListItem]]]
			with ListSubmissionsRequest
			with ListSubmissionsPermissions
			with AutowiringAuditEventQueryServiceComponent
			with Unaudited with ReadOnly
}

trait ListSubmissionsState {
	def assignment: Assignment
}

trait ListSubmissionsRequest extends ListSubmissionsState {
	var checkIndex: Boolean = true
}

abstract class ListSubmissionsCommandInternal(val assignment: Assignment)
	extends CommandInternal[Future[Seq[SubmissionListItem]]]
		with ListSubmissionsState {
	self: ListSubmissionsRequest with AuditEventQueryServiceComponent =>

	override def applyInternal(): Future[Seq[SubmissionListItem]] = {
		for {
			downloads <- if (checkIndex) {
				auditEventQueryService.adminDownloadedSubmissions(assignment)
			} else Future(Seq.empty)
		} yield {
			assignment.submissions.asScala.sortBy(_.submittedDate).reverse.map { submission =>
				SubmissionListItem(submission, downloads.contains(submission))
			}
		}
	}
}

trait ListSubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListSubmissionsState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Submission.Read, mandatory(assignment))
	}
}
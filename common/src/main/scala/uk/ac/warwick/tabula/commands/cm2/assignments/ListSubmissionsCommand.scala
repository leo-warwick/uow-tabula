package uk.ac.warwick.tabula.commands.cm2.assignments

import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionListItem
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.cm2.{AutowiringListSubmissionsCommandResultCache, ListSubmissionsCommandResultCache, ListSubmissionsCommandResultCacheComponent}
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ListSubmissionsCommand {
	type CommandType = Appliable[Seq[SubmissionListItem]] with ListSubmissionsRequest

	def apply(assignment: Assignment) =
		new ListSubmissionsCommandInternal(assignment)
			with ComposableCommand[Seq[SubmissionListItem]]
			with ListSubmissionsRequest
			with ListSubmissionsPermissions
			with AutowiringAuditEventQueryServiceComponent
			with AutowiringListSubmissionsCommandResultCache
			with Unaudited with ReadOnly
}

trait ListSubmissionsState {
	def assignment: Assignment
}

trait ListSubmissionsRequest extends ListSubmissionsState {
	var checkIndex: Boolean = true
}

abstract class ListSubmissionsCommandInternal(val assignment: Assignment)
	extends CommandInternal[Seq[SubmissionListItem]]
		with ListSubmissionsState {
	self: ListSubmissionsRequest with AuditEventQueryServiceComponent with ListSubmissionsCommandResultCacheComponent =>

	override def applyInternal(): Seq[SubmissionListItem] = {
		listSubmissionsCommandResultCache.getValueForKey(assignment, auditEventQueryService.adminDownloadedSubmissions(assignment).map { downloads =>
			assignment.submissions
				.asScala
				.sortBy(_.submittedDate)
				.reverse
				.map(submission => SubmissionListItem(submission, downloads.contains(submission)))
		})

//
//		val downloads =
//			if (checkIndex) try {
//				Await.result(auditEventQueryService.adminDownloadedSubmissions(assignment), 15.seconds)
//			} catch { case timeout: TimeoutException => Nil }
//			else Nil
//
//		submissions.map { submission =>
//			SubmissionListItem(submission, downloads.contains(submission))
//		}
	}
}

trait ListSubmissionsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListSubmissionsState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Submission.Read, mandatory(assignment))
	}
}
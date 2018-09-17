package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Futures
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{AutowiringListFeedbackCommandResultCache, ListFeedbackCommandResultCacheComponent}
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Await
import scala.concurrent.duration._

object ListFeedbackCommand {
	case class ListFeedbackResult(
		downloads: Seq[(User, DateTime)],
		latestOnlineViews: Map[User, DateTime],
		latestOnlineAdded: Map[User, DateTime],
		latestGenericFeedback: Option[DateTime]
	)

	def apply(assignment: Assignment) =
		new ListFeedbackCommandInternal(assignment)
			with ComposableCommand[ListFeedbackResult]
			with ListFeedbackRequest
			with ListFeedbackPermissions
			with UserConversion
			with AutowiringAuditEventQueryServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringTaskSchedulerServiceComponent
			with AutowiringListFeedbackCommandResultCache
			with Unaudited with ReadOnly
}

trait ListFeedbackState {
	def assignment: Assignment
}

trait ListFeedbackRequest extends ListFeedbackState {
	// Empty for now
}

trait UserConversion {
	self: UserLookupComponent =>

	protected def userIdToUser(tuple: (String, DateTime)): (User, DateTime) = tuple match {
		case (id, date) => (userLookup.getUserByUserId(id), date)
	}

	protected def warwickIdToUser(tuple: (String, DateTime)): (User, DateTime) = tuple match {
		case (id, date) => (userLookup.getUserByWarwickUniId(id), date)
	}
}

abstract class ListFeedbackCommandInternal(val assignment: Assignment)
	extends CommandInternal[ListFeedbackResult]
		with ListFeedbackState {
	self: ListFeedbackRequest with UserConversion
		with AuditEventQueryServiceComponent
		with TaskSchedulerServiceComponent
		with ListFeedbackCommandResultCacheComponent =>

	override def applyInternal(): ListFeedbackResult = {

		listFeedbackCommandResultCache.getValueForKey(
			assignment,
			{
				// The time to wait for a query to complete
				val timeout = 15.seconds

				// Wrap each future in Future.optionalTimeout, which will return None if it times out early
				val downloads = Futures.optionalTimeout(auditEventQueryService.feedbackDownloads(assignment), timeout)
				val latestOnlineViews = Futures.optionalTimeout(auditEventQueryService.latestOnlineFeedbackViews(assignment), timeout)
				val latestOnlineAdded = Futures.optionalTimeout(auditEventQueryService.latestOnlineFeedbackAdded(assignment), timeout)
				val latestGenericFeedback = Futures.optionalTimeout(auditEventQueryService.latestGenericFeedbackAdded(assignment), timeout)

				for {
					downloads <- downloads
					latestOnlineViews <- latestOnlineViews
					latestOnlineAdded <- latestOnlineAdded
					latestGenericFeedback <- latestGenericFeedback
				} yield ListFeedbackResult(
					downloads.getOrElse(Nil),
					latestOnlineViews.getOrElse(Map.empty),
					latestOnlineAdded.getOrElse(Map.empty),
					latestGenericFeedback.getOrElse(None)
				)
			}
		)
	}
}

trait ListFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)
	}
}
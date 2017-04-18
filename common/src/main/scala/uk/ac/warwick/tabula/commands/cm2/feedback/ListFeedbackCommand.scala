package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, FeedbackForSits, Module}
import uk.ac.warwick.tabula.helpers.Futures
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Await
import scala.concurrent.duration._

// FIXME: implemented as part of CM2 migration but will require further reworking due to CM2 workflow changes

object ListFeedbackCommand {
	case class ListFeedbackResult(
		downloads: Seq[(User, DateTime)],
		latestOnlineViews: Map[User, DateTime],
		latestOnlineAdded: Map[User, DateTime],
		latestGenericFeedback: Option[DateTime]
	)

	case class FeedbackListItem(feedback: Feedback, downloaded: Boolean, onlineViewed: Boolean, feedbackForSits: FeedbackForSits)

	def apply(module: Module, assignment: Assignment) =
		new ListFeedbackCommandInternal(module, assignment)
			with ComposableCommand[ListFeedbackResult]
			with ListFeedbackRequest
			with ListFeedbackPermissions
			with UserConversion
			with AutowiringAuditEventQueryServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringTaskSchedulerServiceComponent
			with Unaudited with ReadOnly
}

trait ListFeedbackState {
	def module: Module
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

abstract class ListFeedbackCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[ListFeedbackResult]
		with ListFeedbackState {
	self: ListFeedbackRequest with UserConversion
		with AuditEventQueryServiceComponent
		with TaskSchedulerServiceComponent =>

	override def applyInternal(): ListFeedbackResult = {
		// The time to wait for a query to complete
		val timeout = 15.seconds

		// Wrap each future in Future.optionalTimeout, which will return None if it times out early
		val downloads = Futures.optionalTimeout(auditEventQueryService.feedbackDownloads(assignment), timeout)
		val latestOnlineViews = Futures.optionalTimeout(auditEventQueryService.latestOnlineFeedbackViews(assignment), timeout)
		val latestOnlineAdded = Futures.optionalTimeout(auditEventQueryService.latestOnlineFeedbackAdded(assignment), timeout)
		val latestGenericFeedback = Futures.optionalTimeout(auditEventQueryService.latestGenericFeedbackAdded(assignment), timeout)

		val result = for {
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

		// We arbitrarily wait a longer time for the result, safe in the knowledge that if they don't return in a reasonable
		// time then we've messed up.
		Await.result(result, timeout * 2)
	}
}

trait ListFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)
	}
}
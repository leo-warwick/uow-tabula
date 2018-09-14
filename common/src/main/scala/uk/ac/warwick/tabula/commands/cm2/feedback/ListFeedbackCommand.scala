package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future

object ListFeedbackCommand {
	case class ListFeedbackResult(
		downloads: Seq[(User, DateTime)],
		latestOnlineViews: Map[User, DateTime],
		latestOnlineAdded: Map[User, DateTime],
		latestGenericFeedback: Option[DateTime]
	)

	def apply(assignment: Assignment) =
		new ListFeedbackCommandInternal(assignment)
			with ComposableCommand[Future[ListFeedbackResult]]
			with ListFeedbackRequest
			with ListFeedbackPermissions
			with UserConversion
			with AutowiringAuditEventQueryServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringTaskSchedulerServiceComponent
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
	extends CommandInternal[Future[ListFeedbackResult]]
		with ListFeedbackState {
	self: ListFeedbackRequest with UserConversion
		with AuditEventQueryServiceComponent
		with TaskSchedulerServiceComponent =>

	override def applyInternal(): Future[ListFeedbackResult] = {
		for {
			downloads <- auditEventQueryService.feedbackDownloads(assignment)
			latestOnlineViews <- auditEventQueryService.latestOnlineFeedbackViews(assignment)
			latestOnlineAdded <- auditEventQueryService.latestOnlineFeedbackAdded(assignment)
			latestGenericFeedback <- auditEventQueryService.latestGenericFeedbackAdded(assignment)
		} yield {
			ListFeedbackResult(
				downloads,
				latestOnlineViews,
				latestOnlineAdded,
				latestGenericFeedback
			)
		}
	}
}

trait ListFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)
	}
}
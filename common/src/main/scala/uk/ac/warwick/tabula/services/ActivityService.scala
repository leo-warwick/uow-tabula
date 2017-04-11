package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Activity, Module}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventNoteworthySubmissionsService, PagedAuditEvents}

import scala.concurrent.Future
import scala.language.implicitConversions

object ActivityService {
	case class PagedActivities(items: Seq[Activity[_]], lastUpdatedDate: Option[DateTime], totalHits: Long)

	implicit def pagedAuditEventsToActivities(in: Future[PagedAuditEvents]): Future[PagedActivities] =
		in.map { pagedEvents =>
			PagedActivities(pagedEvents.items.flatMap(Activity.apply), pagedEvents.lastUpdatedDate, pagedEvents.totalHits)
		}
}

/** At the moment, this uses AuditEvents as a proxy for things of interest,
 *  and specifically is only noticing new submission events.
 *  In the future it'll likely make sense to serve events of interest at whichever
 *  depth of Tabula we're looking from, and look directly at typed Activity[A] rather
 *  than just Activity[AuditEvent]
 * */
@Service
class ActivityService {
	import ActivityService._

	private val StreamSize = 8

	var moduleService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	var assignmentService: AssessmentService = Wire[AssessmentService]
	var auditQueryService: AuditEventNoteworthySubmissionsService = Wire[AuditEventNoteworthySubmissionsService]

	// first page
	def getNoteworthySubmissions(user: CurrentUser): Future[PagedActivities] =
		auditQueryService.noteworthySubmissionsForModules(getModules(user), None, StreamSize)

	// following pages
	def getNoteworthySubmissions(user: CurrentUser, lastUpdatedDate: DateTime): Future[PagedActivities] =
		auditQueryService.noteworthySubmissionsForModules(getModules(user), Some(lastUpdatedDate), StreamSize)

	private def getModules(user: CurrentUser): Seq[Module] = {
		val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		val adminModules = moduleService.modulesInDepartmentsWithPermission(user, Permissions.Module.ManageAssignments)

		(ownedModules ++ adminModules).toSeq
	}
}

trait ActivityServiceComponent {
	def activityService: ActivityService
}

trait AutowiringActivityServiceComponent extends ActivityServiceComponent {
	var activityService: ActivityService = Wire[ActivityService]
}
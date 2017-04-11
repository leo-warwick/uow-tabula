package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventQueryServiceComponent, AutowiringAuditEventQueryServiceComponent}
import uk.ac.warwick.tabula.services.{AuditEventServiceComponent, AutowiringAuditEventServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.sysadmin.AuditLogQueryCommand._

import scala.concurrent.Await
import scala.concurrent.duration._

object AuditLogQueryCommand {

	type AuditLogQueryCommand = Appliable[AuditLogQueryResults] with AuditLogQueryCommandState

	val pageSize = 100

	def apply(): AuditLogQueryCommand =
		new AuditLogQueryCommandInternal
			with ComposableCommand[AuditLogQueryResults]
			with AuditLogQueryCommandPermissions
			with AutowiringAuditEventServiceComponent
			with AutowiringAuditEventQueryServiceComponent
			with Unaudited with ReadOnly

	case class AuditLogQueryResults(
		results: Seq[AuditEvent],
		pageNumber: Int,
		startIndex: Int,
		endIndex: Int
	)
}

class AuditLogQueryCommandInternal extends CommandInternal[AuditLogQueryResults] with AuditLogQueryCommandState with TaskBenchmarking {
	self: AuditEventServiceComponent with AuditEventQueryServiceComponent =>

	def applyInternal(): AuditLogQueryResults = {
		val start = (page * pageSize) + 1
		val max = pageSize
		val end = start + max - 1
		val recentFuture =
			if (query.hasText) {
				auditEventQueryService.query(query, page * pageSize, pageSize)
			} else {
				auditEventQueryService.listRecent(page * pageSize, pageSize)
			}

		val recent = benchmarkTask("Query audit event index") {
			Await.result(recentFuture, 15.seconds)
		}

		AuditLogQueryResults(
			results = benchmarkTask("Parse into rich audit items") { recent.map(toRichAuditItem) },
			pageNumber = page,
			startIndex = start,
			endIndex = end
		)
	}

	def toRichAuditItem(item: AuditEvent): AuditEvent = item.copy(parsedData = auditEventService.parseData(item.data))
}

trait AuditLogQueryCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ViewAuditLog)
	}
}

trait AuditLogQueryCommandState {
	var page: Int = 0
	var query: String = ""
}

@Controller
@RequestMapping(Array("/sysadmin/audit/search"))
class AuditLogController extends BaseSysadminController {

	@ModelAttribute("auditLogQuery") def command: AuditLogQueryCommand = AuditLogQueryCommand()

	@annotation.RequestMapping
	def searchAll(@ModelAttribute("auditLogQuery") command: AuditLogQueryCommand): Mav = {
		val results = command.apply()

		Mav("sysadmin/audit/list",
			"items" -> results.results,
			"fromIndex" -> true,
			"page" -> results.pageNumber,
			"startIndex" -> results.startIndex,
			"endIndex" -> results.endIndex)
	}

}
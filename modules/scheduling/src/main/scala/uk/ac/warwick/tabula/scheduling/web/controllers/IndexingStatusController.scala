package uk.ac.warwick.tabula.scheduling.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.tabula.services.{AuditEventIndexService, AuditEventService}
import uk.ac.warwick.spring.Wire

import scala.concurrent.Await
import scala.concurrent.duration._

@Controller
@RequestMapping(Array("/index/nagios"))
class IndexingStatusController extends BaseController {
	import IndexingStatusController._

	var auditEventService = Wire[AuditEventService]
	var auditEventIndexService = Wire[AuditEventIndexService]

	@RequestMapping(Array("/status"))
	def lastrun(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		val latestDb = auditEventService.latest
		val latestIndex = Await.result(auditEventIndexService.latestIndexItem, 1.second)
		val latestIndexMinutesAgo = (latestDb.get.getMillis - latestIndex.getMillis) / MillisInAMinute

		val allDetails = s"latestDatabase,${latestDb.get.getMillis},latestIndex,${latestIndex.getMillis},latestIndexMinutesAgo,${latestIndexMinutesAgo}"

		response.addHeader("Content-Type", "text/plain")
		response.addHeader("Content-Length", allDetails.length.toString)
		response.getWriter().write(allDetails)
	}

}

object IndexingStatusController {
	val MillisInASecond = 1000
	val MillisInAMinute = MillisInASecond * 60
}
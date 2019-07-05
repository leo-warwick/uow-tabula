package uk.ac.warwick.tabula.web.controllers.home

import java.util

import javax.servlet.http.HttpServletRequest
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/csp-report"))
class CspReportController extends BaseController {

  val SecurityLogger: Logger = LoggerFactory.getLogger("uk.ac.warwick.SECURITY_REPORTS")

  @PostMapping(consumes = Array(MediaType.APPLICATION_JSON_VALUE, "application/csp-report"), produces = Array("application/json"))
  def consumeReport(@RequestBody report: Map[String, Any], request: HttpServletRequest): Mav = {
    val data = Map(
      "csp-report" -> report.get("csp-report"),
      "request_headers" -> Map(
        "user-agent" -> request.getHeader("User-Agent").maybeText
      )
    )

    SecurityLogger.info("{}", StructuredArguments.entries(Logging.convertForStructuredArguments(data).asInstanceOf[util.Map[String, Object]]))

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

  @PostMapping(consumes = Array("application/reports+json"), produces = Array("application/json"))
  def consumeReport(@RequestBody reports: Seq[Map[String, Any]]): Mav = {
    reports.filter { r => r.get("type").contains("csp") && r.contains("body") }.foreach { report =>
      val data = Map(
        "csp-report" -> report.get("body"),
        "request_headers" -> Map(
          "user-agent" -> report.get("user_agent")
        )
      )

      SecurityLogger.info("{}", StructuredArguments.entries(Logging.convertForStructuredArguments(data).asInstanceOf[util.Map[String, Object]]))
    }

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok"
    )))
  }

}

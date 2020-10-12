package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands.{Command, TaskBenchmarking}
import uk.ac.warwick.tabula.helpers.Logging

class RequestBenchmarkingInterceptor extends HandlerInterceptorAdapter with TaskBenchmarking {

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any): Boolean = {
    if (Logging.benchmarking) {
      val description = RequestInfo.fromThread.map { info =>
        val userId =
          if (info.user.masquerading) s"${info.user.apparentId} (really ${info.user.realId})"
          else if (info.user.exists) info.user.realId
          else "anon"

        val url = info.requestedUri

        "[%s] %s".format(userId, url)
      }.getOrElse("[unknown request]")

      val stopWatch = Command.getOrInitStopwatch()
      stopWatch.start(description)
    }

    true
  }

  override def afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Object, ex: Exception): Unit = {
    if (Logging.benchmarking) {
      val stopwatch = Command.getOrInitStopwatch()
      stopwatch.stop()

      if (stopwatch.getTotalTimeMillis > Command.MillisToSlowlog) {
        Command.slowLogger.warn(stopwatch.prettyPrint)
      }

      Command.endStopwatching()
    }
  }

}

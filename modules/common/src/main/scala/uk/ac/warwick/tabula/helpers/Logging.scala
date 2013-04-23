package uk.ac.warwick.tabula.helpers
import org.apache.log4j.Logger
import Stopwatches._
import org.apache.log4j.Priority
import scala.reflect.ClassTag
import org.apache.log4j.Level

trait Logging {
	@transient val loggerName = this.getClass.getName
	@transient lazy val logger = Logger.getLogger(loggerName)
	@transient lazy val debugEnabled = logger.isDebugEnabled

	@transient val Info = Level.INFO
	@transient val Debug = Level.DEBUG

	/**
	 * Logs a debug message, with the given arguments inserted into the
	 * format placeholders in the message. Checks debugEnabled for you,
	 * so no need to do that.
	 */
	def debug(message: String, arguments: Any*) =
		if (debugEnabled) logger.debug(message format (arguments: _*))

	/**
	 * Log an info message with the size of a collection.
	 * Returns the collection so you can wrap it with this without having
	 * to break out into a variable.
	 */
	def logSize[A](seq: Seq[A], level: Priority = Info)(implicit tag: ClassTag[A]) = {
		logger.log(level, "Collection of " + tag.runtimeClass.getSimpleName + "s: " + seq.size)
		seq
	}

	/**
	 * For logging the result of a function without having to break it
	 * out into multiple lines.
	 */
	def debugResult[A](description: String, result: A): A = {
		debug("%s: %s", description, result)
		result
	}

	/**
	 * Wrap some code in a stopwatch, logging some timing info
	 * if it takes longer than minMillis.
	 */
	def benchmark[A](description: String, level: Priority = Info, minMillis: Int = 0)(fn: => A): A = {
		if (Logging.benchmarking) {
			val s = StopWatch()
			try s.record(description) {
				fn
			} finally {
				if (s.getTotalTimeMillis > minMillis) {
					logger.log(level, s.prettyPrint)
				}
			}
		} else {
			fn
		}
	}
	
	/**
	 * The same as benchmark, but passes the StopWatch as a callback to the function
	 */
	def timed[A](description: String, level: Priority = Info, minMillis: Int = 0)(fn: => (uk.ac.warwick.util.core.StopWatch => A)): A = {
		val s = StopWatch()
		try s.record(description) {
			fn(s)
		} finally {
			if (s.getTotalTimeMillis > minMillis) {
				logger.log(level, s.prettyPrint)
			}
		}
	}

}

object Logging {
	var benchmarking = true
}

package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util
import language.implicitConversions

object Stopwatches {

	class EnhancedStopwatch(val stopwatch: util.core.StopWatch) {
		def record[A](taskName: String)(work: => A): A = {
			try {
				stopwatch.start(taskName)
				work
			} finally {
				stopwatch.stop()
			}
		}

		def isFinished: Boolean = !stopwatch.hasRunningTask()
	}

	implicit def ToEnhancedStopwatch(s: util.core.StopWatch) = new EnhancedStopwatch(s)

	object StopWatch {
		def apply() = new util.core.StopWatch()
	}
}
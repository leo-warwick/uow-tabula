package uk.ac.warwick.tabula.helpers

import org.joda.time.DateTime

trait ClockComponent {
	trait Clock {
		def now:DateTime
	}
	val clock: Clock
}
trait SystemClockComponent extends ClockComponent {
	val clock = new Clock{
		def now:DateTime = DateTime.now
	}
}

// good for testing, and also for situations where you want to ensure the same timestamp is used on
// a number of objects in the same transaction
trait StoppedClockComponent extends ClockComponent {
	val stoppedTime:DateTime
	val clock = new Clock{
		def now:DateTime = stoppedTime
	}
}
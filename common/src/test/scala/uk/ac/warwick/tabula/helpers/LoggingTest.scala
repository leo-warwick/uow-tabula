package uk.ac.warwick.tabula.helpers

import org.junit.After
import uk.ac.warwick.tabula.{TestBase, TestLoggerFactory}

import scala.collection.JavaConverters._
import ch.qos.logback.classic.{Level, Logger}

// scalastyle:off magic.number
class LoggingTest extends TestBase with Logging {

	val testLogger: Logger = TestLoggerFactory.getTestLogger(logger.getName)

	@Test def itWorks {
		testLogger should be (logger)

		debug("my message")

		debug("my message %s (%s)", "my argument 1", "my argument 2")

		logSize(Seq("a", "b", "c"))

		debugResult("my magical thing", true)

		TestLoggerFactory.retrieveEvents(testLogger).map(e => (e.getLevel, e.getMessage)) should be (Seq(
			(Level.DEBUG, "my message"),
			(Level.DEBUG, "my message my argument 1 (my argument 2)"),
			(Level.INFO, "Collection of Strings: 3"),
			(Level.DEBUG, "my magical thing: true")
		))
//		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my message my argument 1 (my argument 2)"))
//		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.info("Collection of Strings: 3"))
//		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my magical thing: true"))

		// Turn benchmarking off
		Logging.benchmarking = false
		var ranFn = false
		benchmark("my magical thing") {
			Thread.sleep(50)
			ranFn = true
		}

		// The last thing is still there because benchmarking is disabled, but we still ran the function
		ranFn should be (true)
		TestLoggerFactory.retrieveEvents(testLogger).map(e => (e.getLevel, e.getMessage)).last should be (
			(Level.DEBUG, "my magical thing: true")
		)
	}

}
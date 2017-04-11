package uk.ac.warwick.tabula.system.exceptions

import org.junit.After

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{ItemNotFoundException, TestBase, TestLoggerFactory}
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import ch.qos.logback.classic.{Level, Logger}

class LoggingExceptionHandlerTest extends TestBase {

	val testLogger: Logger = TestLoggerFactory.getTestLogger(classOf[LoggingExceptionHandler].getName)
	val handler = new LoggingExceptionHandler
	val lineSeparator: String = System.getProperty("line.separator")

	@Test def userError {
		val context = ExceptionContext("1", new ItemNotFoundException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = TestLoggerFactory.retrieveEvents(testLogger).last
		logEvent.getMessage.split(lineSeparator).head should be ("User error")
		logEvent.getLevel should be (Level.DEBUG)
	}

	@Test def handledException {
		val context = ExceptionContext("1", new MaintenanceModeEnabledException(None, None), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = TestLoggerFactory.retrieveEvents(testLogger).last
		logEvent.getMessage.split(lineSeparator).head should be ("Handled exception")
		logEvent.getLevel should be (Level.DEBUG)
	}

	@Test def normalException {
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = TestLoggerFactory.retrieveEvents(testLogger).last
		logEvent.getMessage.split(lineSeparator).head should be ("Exception 1")
		logEvent.getLevel should be (Level.ERROR)
	}

}
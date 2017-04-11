package uk.ac.warwick.tabula

import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.spi.ILoggingEvent
import org.slf4j.LoggerFactory
import ch.qos.logback

import scala.collection.JavaConverters._

object TestLoggerFactory {

	type Appender = logback.core.read.ListAppender[ILoggingEvent]

	private val loggers = new ThreadLocal[List[logback.classic.Logger]] {
		override def initialValue = Nil
	}

	def getTestLogger(name: String): Logger = {
		val logger = LoggerFactory.getLogger(name).asInstanceOf[logback.classic.Logger]
		val appender = new Appender
		appender.start()
		appender.setName("TestLogAppender")
		logger.detachAndStopAllAppenders()
		logger.setLevel(Level.DEBUG)
		logger.addAppender(appender)
		storeLogger(logger)
		logger
	}

	def retrieveEvents(logger: logback.classic.Logger) : Seq[ILoggingEvent] = {
		val appender = logger.iteratorForAppenders().next.asInstanceOf[Appender]
		appender.list.asScala.toSeq
	}

	def tearDown() : Unit = {
		for (logger <- loggers.get) {
			logger.detachAndStopAllAppenders()
		}
		loggers.remove()
	}

	private def storeLogger(logger: logback.classic.Logger) = {
		val list = loggers.get match {
			case null => List(logger)
			case tail => logger :: tail
		}
		loggers.set(list)
	}

}

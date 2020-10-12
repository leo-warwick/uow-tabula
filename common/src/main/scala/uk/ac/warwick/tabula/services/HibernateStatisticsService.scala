package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.hibernate.stat.Statistics
import org.joda.time.format.DateTimeFormat
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

import scala.beans.BeanProperty

/**
  * Listens for messages
  */
@Component
class HibernateStatisticsService extends QueueListener with InitializingBean with Logging with Daoisms {

  private val DateFormat = DateTimeFormat.shortDateTime()

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")
  var context: String = Wire.property("${module.context}")

  override def isListeningToQueue = true

  override def onReceive(item: Any): Unit = {
    logger.info("Updating sessionFactory statistics options " + item + " for " + context)
    item match {
      case HibernateStatisticsMessage("enable") => sessionFactory.getStatistics.setStatisticsEnabled(true)
      case HibernateStatisticsMessage("disable") => sessionFactory.getStatistics.setStatisticsEnabled(false)
      case HibernateStatisticsMessage("clear") => sessionFactory.getStatistics.clear()
      case HibernateStatisticsMessage("log") => logger.info("Hibernate stats for " + context + ": " + getStatisticsSummary(sessionFactory.getStatistics))
    }
  }

  def getStatisticsSummary(stats: Statistics): String = {
    s"""
       		|Stats enabled: ${stats.isStatisticsEnabled} (since ${DateFormat.print(stats.getStartTime)})
       		|
       		|Flushes: ${stats.getFlushCount}
       		|Entity loads: ${stats.getEntityLoadCount}
       		|Entity fetches: ${stats.getEntityFetchCount}
       		|Collection loads: ${stats.getCollectionLoadCount}
       		|
       		|Sessions opened: ${stats.getSessionOpenCount}
       		|Sessions closed: ${stats.getSessionCloseCount}
       		|
       		|Query count: ${stats.getQueryExecutionCount}
       		|Slowest query (${stats.getQueryExecutionMaxTime}ms): ${stats.getQueryExecutionMaxTimeQueryString}
		""".stripMargin
  }

  override def afterPropertiesSet(): Unit = {
    logger.debug("Registering listener for " + classOf[HibernateStatisticsMessage].getAnnotation(classOf[ItemType]).value + " on " + context)
    queue.addListener(classOf[HibernateStatisticsMessage].getAnnotation(classOf[ItemType]).value, this)
  }

}

@JsonAutoDetect
@ItemType("HibernateStatistics")
case class HibernateStatisticsMessage(@BeanProperty var action: String) {
  def this() = {
    this(null)
  }
}

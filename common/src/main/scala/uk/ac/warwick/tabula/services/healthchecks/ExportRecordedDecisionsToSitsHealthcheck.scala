package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.healthchecks.ExportToSitsHealthcheck._
import uk.ac.warwick.tabula.services.marks.{DecisionService, ResitService}
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration.Duration.Zero
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag


trait WriteToSits {
  def updatedDate: DateTime
}

trait SitsQueueService {
  def allNeedingWritingToSits: Seq[WriteToSits]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

object ExportToSitsHealthcheck {
  val QueueSizeWarningThreshold = 1000
  val QueueSizeErrorThreshold = 2000
  val DelayWarningThreshold: Duration = 30.minutes
  val DelayErrorThreshold: Duration = 1.hour
}



abstract class ExportToSitsHealthcheck[A >: Null <: SitsQueueService : ClassTag](initialState: ServiceHealthcheck) extends ServiceHealthcheckProvider(initialState) {
  def entityName: String
  def healthcheckName: String

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    val service = Wire[A]
    val queue: Seq[WriteToSits] = service.allNeedingWritingToSits
    val queueSize = queue.size

    val countStatus =
      if (queueSize >= QueueSizeErrorThreshold) ServiceHealthcheck.Status.Error
      else if (queueSize >= QueueSizeWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay

    val countMessage =
      s"$queueSize $entityName${if (queueSize == 1) "" else "s"} in queue" +
        (if (countStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (countStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
        s" (warning: $QueueSizeWarningThreshold, critical: $QueueSizeErrorThreshold)"

    // How old is the oldest item in the queue?
    val oldestUnwrittenDelay =
      queue.minByOption(_.updatedDate).map { mark =>
        Minutes.minutesBetween(mark.updatedDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val mostRecentlyWrittenDelay =
      service.mostRecentlyWrittenToSitsDate.map { syncDate =>
        Minutes.minutesBetween(syncDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val delayStatus =
      if (oldestUnwrittenDelay == Zero) ServiceHealthcheck.Status.Okay // empty queue
      else if (mostRecentlyWrittenDelay >= DelayErrorThreshold) ServiceHealthcheck.Status.Error
      else if (mostRecentlyWrittenDelay >= DelayWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay // queue still processing so may take time to sent them all

    val delayMessage =
      s"Last written $entityName $mostRecentlyWrittenDelay ago, oldest unwritten $entityName $oldestUnwrittenDelay old " +
        (if (delayStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (delayStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
        s"(warning: $DelayWarningThreshold, critical: $DelayErrorThreshold)"

    val status = Seq(countStatus, delayStatus).maxBy(_.ordinal())

    update(new ServiceHealthcheck(
      healthcheckName,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"$countMessage. $delayMessage",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("queue_size", queueSize, QueueSizeWarningThreshold, QueueSizeErrorThreshold),
        new ServiceHealthcheck.PerformanceData("oldest_written", oldestUnwrittenDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes),
        new ServiceHealthcheck.PerformanceData("last_written", mostRecentlyWrittenDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes)
      ).asJava
    ))
  }

}


object ExportRecordedDecisionsToSitsHealthcheck {
  val Name = "export-decision-to-sits"
}

@Component
@Profile(Array("scheduling"))
class ExportRecordedDecisionsToSitsHealthcheck extends ExportToSitsHealthcheck[DecisionService](
  new ServiceHealthcheck(ExportRecordedDecisionsToSitsHealthcheck.Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))
) {
  val entityName: String = "decision"
  override def healthcheckName: String = ExportRecordedDecisionsToSitsHealthcheck.Name
}

object ExportRecordedResitsToSitsHealthcheck {
  val Name = "export-resit-to-sits"
}

@Component
@Profile(Array("scheduling"))
class ExportRecordedResitsToSitsHealthcheck extends ExportToSitsHealthcheck[ResitService](
  new ServiceHealthcheck(ExportRecordedResitsToSitsHealthcheck.Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))
) {
  val entityName: String = "resit"
  override def healthcheckName: String = ExportRecordedResitsToSitsHealthcheck.Name
}

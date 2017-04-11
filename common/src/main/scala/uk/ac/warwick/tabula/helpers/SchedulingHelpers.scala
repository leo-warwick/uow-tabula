package uk.ac.warwick.tabula.helpers

import org.quartz.TriggerBuilder._
import org.quartz.{TriggerKey, JobDataMap, JobKey, Scheduler}
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.reflect._

trait SchedulingHelpers {
	class SuperScheduler(scheduler: Scheduler) {
		def scheduleNow[J <: AutowiredJobBean : ClassTag](data: (String, Any)*): TriggerKey = {
			val trigger =
				newTrigger()
					.forJob(new JobKey(classTag[J].runtimeClass.getSimpleName))
					.usingJobData(new JobDataMap(data.toMap.asJava))
					.startNow()
					.build()

			scheduler.scheduleJob(trigger)

			trigger.getKey
		}
	}

	implicit def SchedulerToSuperScheduler(scheduler: Scheduler): SuperScheduler = new SuperScheduler(scheduler)
}

object SchedulingHelpers extends SchedulingHelpers
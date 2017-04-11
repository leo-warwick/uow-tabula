package uk.ac.warwick.tabula.services

import java.util.concurrent.{Executors, ScheduledExecutorService}

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean

@Service
class TaskSchedulerService extends ScalaFactoryBean[ScheduledExecutorService] {
	override def createInstance(): ScheduledExecutorService = Executors.newScheduledThreadPool(2)
	override def destroyInstance(instance: ScheduledExecutorService): Unit = instance.shutdown()
}

trait TaskSchedulerServiceComponent {
	implicit def taskSchedulerService: ScheduledExecutorService
}

trait AutowiringTaskSchedulerServiceComponent extends TaskSchedulerServiceComponent {
	override implicit val taskSchedulerService: ScheduledExecutorService = Wire[ScheduledExecutorService]
}
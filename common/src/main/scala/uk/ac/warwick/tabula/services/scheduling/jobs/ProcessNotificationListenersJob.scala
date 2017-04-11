package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{JobExecutionContext, DisallowConcurrentExecution}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Scope, Profile}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessNotificationListenersJob extends AutowiredJobBean {

	@Autowired var notificationService: NotificationService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingProcessNotificationListeners)
			exceptionResolver.reportExceptions {
				notificationService.processListeners()
			}
	}

}

package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.EmailNotificationService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessEmailQueueJob extends AutowiredJobBean {

	@Autowired var emailNotificationService: EmailNotificationService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingNotificationEmails)
			exceptionResolver.reportExceptions {
				emailNotificationService.processNotifications()
			}
	}

}

package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessJobQueueJob extends AutowiredJobBean {

	@Autowired var jobService: JobService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingJobService)
			exceptionResolver.reportExceptions {
				jobService.run()
			}
	}

}

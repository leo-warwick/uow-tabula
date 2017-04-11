package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext, Scheduler}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportProfilesCommand, StampMissingRowsCommand, StampMissingRowsCommandInternal}
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class StampMissingRowsJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingProfilesImport)
			exceptionResolver.reportExceptions {
				val cmd = StampMissingRowsCommand()
				cmd.apply()
			}

	}

}

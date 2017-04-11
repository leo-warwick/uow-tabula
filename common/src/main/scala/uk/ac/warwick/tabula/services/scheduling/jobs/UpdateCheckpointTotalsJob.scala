package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.scheduling.UpdateAttendanceMonitoringCheckpointTotalsCommand
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class UpdateCheckpointTotalsJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingAttendanceUpdateTotals) {
			exceptionResolver.reportExceptions {
				val command = UpdateAttendanceMonitoringCheckpointTotalsCommand()
				val errors = new BindException(command, "command")
				command.validate(errors)
				if (!errors.hasErrors) {
					command.apply()
				}
			}
		}
	}

}
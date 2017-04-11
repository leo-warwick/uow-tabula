package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext, Scheduler}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportProfilesJob extends AutowiredJobBean {

	private val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
	private val scheduler = Wire[Scheduler]

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingProfilesImport)
			exceptionResolver.reportExceptions {
				moduleAndDepartmentService.allDepartments.foreach(dept =>
					scheduler.scheduleNow[ImportProfilesSingleDepartmentJob]("departmentCode" -> dept.code)
				)
			}
	}

}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportProfilesSingleDepartmentJob extends AutowiredJobBean with Logging {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingProfilesImport)
			exceptionResolver.reportExceptions {
				val deptCode = Option(context.getMergedJobDataMap.getString("departmentCode")).flatMap(_.maybeText)
				if (deptCode.isEmpty) {
					logger.error("Tried to import profiles for a department, but no department code was found.")
				} else {
					val cmd = new ImportProfilesCommand()
					cmd.deptCode = deptCode.get
					cmd.apply()
				}
			}

	}

}

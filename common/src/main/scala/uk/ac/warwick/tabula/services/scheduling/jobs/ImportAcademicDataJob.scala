package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportDepartmentsModulesCommand, ImportAcademicInformationCommand}
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.helpers.StringUtils._

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportAcademicDataJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingAcademicInformationImport)
			exceptionResolver.reportExceptions {
				// If we get department codes, just do module import for that department
				context.getMergedJobDataMap.getString("departmentCodes").maybeText match {
					case Some(deptCodes) =>
						val cmd = ImportDepartmentsModulesCommand()
						cmd.deptCode = deptCodes
						cmd.apply()
					case _ => ImportAcademicInformationCommand().apply()
				}
			}
	}

}

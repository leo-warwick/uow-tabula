package uk.ac.warwick.tabula.services.scheduling.jobs

import org.joda.time.DateTime
import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.{UnlinkAttendanceMonitoringSchemeCommand, UpdateAttendanceMonitoringSchemeMembershipCommand}
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class UpdateMonitoringPointSchemeMembershipJob extends AutowiredJobBean {

	implicit var termService: TermService = Wire[TermService]

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingAttendanceUpdateSchemes) {
			exceptionResolver.reportExceptions {
				UpdateAttendanceMonitoringSchemeMembershipCommand().apply()
			}
			exceptionResolver.reportExceptions {
				val thisAcademicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now)
				if (thisAcademicYear.isSITSInFlux(DateTime.now)) {
					UnlinkAttendanceMonitoringSchemeCommand().apply()
				}
			}
		}
	}

}
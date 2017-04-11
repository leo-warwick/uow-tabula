package uk.ac.warwick.tabula.services.scheduling

import java.util.Properties
import javax.sql.DataSource

import org.quartz._
import org.springframework.beans.factory.annotation.{Autowired, Qualifier, Value}
import org.springframework.beans.factory.{FactoryBean, InitializingBean}
import org.springframework.context.annotation.{Bean, Configuration, Profile}
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scheduling.quartz.{JobDetailFactoryBean, QuartzJobBean, SchedulerFactoryBean, SpringBeanJobFactory}
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.services.scheduling.SchedulingConfiguration.{ScheduledJob, UnscheduledJob}
import uk.ac.warwick.tabula.services.scheduling.jobs._
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.util.core.spring.scheduling.{AutowiringSpringBeanJobFactory, PersistableCronTriggerFactoryBean, PersistableSimpleTriggerFactoryBean}
import uk.ac.warwick.util.web.Uri

import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect._

object SchedulingConfiguration {
	abstract class UnscheduledJob[J <: AutowiredJobBean : ClassTag] {
		def name: String

		lazy val jobDetail: JobDetail = {
			val jobDetail = new JobDetailFactoryBean
			jobDetail.setName(name)
			jobDetail.setJobClass(classTag[J].runtimeClass)
			jobDetail.setDurability(true)
			jobDetail.setRequestsRecovery(true)
			jobDetail.afterPropertiesSet()
			jobDetail.getObject
		}
	}

	case class SimpleUnscheduledJob[J <: AutowiredJobBean : ClassTag](jobName: Option[String] = None) extends UnscheduledJob[J] {
		lazy val name: String = jobName.getOrElse(classTag[J].runtimeClass.getSimpleName)
	}

	abstract class ScheduledJob[J <: AutowiredJobBean : ClassTag, T <: Trigger] extends UnscheduledJob[J] {
		def trigger: T
	}

	case class SimpleTriggerJob[J <: AutowiredJobBean : ClassTag](
		repeatInterval: Duration,
		jobName: Option[String] = None,
		misfireInstruction: Int = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
	) extends ScheduledJob[J, SimpleTrigger] {
		lazy val name: String = jobName.getOrElse(classTag[J].runtimeClass.getSimpleName)

		lazy val trigger: SimpleTrigger = {
			val trigger = new PersistableSimpleTriggerFactoryBean
			trigger.setName(name)
			trigger.setJobDetail(jobDetail)
			if (repeatInterval > 0.seconds) {
				trigger.setRepeatInterval(repeatInterval.toMillis)
			} else {
				trigger.setRepeatInterval(1.minute.toMillis) // Have to set something or it defaults to zero and throws
				trigger.setRepeatCount(0)
			}
			trigger.setMisfireInstruction(misfireInstruction)
			trigger.afterPropertiesSet()
			trigger.getObject
		}
	}

	case class CronTriggerJob[J <: AutowiredJobBean : ClassTag](
		cronExpression: String,
		jobName: Option[String] = None,
		misfireInstruction: Int = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING
	) extends ScheduledJob[J, CronTrigger] {
		lazy val name: String = jobName.getOrElse(classTag[J].runtimeClass.getSimpleName)

		lazy val trigger: CronTrigger = {
			val trigger = new PersistableCronTriggerFactoryBean
			trigger.setName(name)
			trigger.setJobDetail(jobDetail)
			trigger.setCronExpression(cronExpression)
			trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING)
			trigger.afterPropertiesSet()
			trigger.getObject
		}
	}

	val unscheduledJobs: Seq[UnscheduledJob[_]] = Seq(
		SimpleUnscheduledJob[ImportProfilesSingleDepartmentJob](),
		SimpleUnscheduledJob[ImportAssignmentsAllYearsJob]()
	)

	/**
		* Be very careful about changing the names of jobs here. If a job with a name is no longer referenced,
		* we will clear out ALL Quartz data the next time that the application starts, which may have unintended
		* side-effects (but will probably be fine). Adding new jobs is fine, it's just dereferencing or renaming
		* that's awkward.
		*
		* @see http://codrspace.com/Khovansa/spring-quartz-with-a-database/
		*/
	val scheduledJobs: Seq[ScheduledJob[_, _ <: Trigger]] = Seq(
		// Imports
		CronTriggerJob[ImportAcademicDataJob](cronExpression = "0 0 7,14 * * ?"), // 7am and 2pm
		CronTriggerJob[ImportProfilesJob](cronExpression = "0 30 0 * * ?"), // 12:30am
		CronTriggerJob[StampMissingRowsJob](cronExpression = "23 0 0 * * ?"), // 11:00pm
		CronTriggerJob[ImportAssignmentsJob](cronExpression = "0 0 7 * * ?"), // 7am
		CronTriggerJob[ImportModuleListsJob](cronExpression = "0 0 8 * * ?"), // 8am

		CronTriggerJob[CleanupTemporaryFilesJob](cronExpression = "0 0 2 * * ?"), // 2am

		CronTriggerJob[UpdateMonitoringPointSchemeMembershipJob](cronExpression = "0 0 4 * * ?"), // 4am
		CronTriggerJob[UpdateLinkedDepartmentSmallGroupSetJob](cronExpression = "0 0 5 * * ?"), // 5am

		SimpleTriggerJob[ProcessScheduledNotificationsJob](repeatInterval = 1.minute),
		SimpleTriggerJob[ProcessTriggersJob](repeatInterval = 10.seconds),

		SimpleTriggerJob[ProcessEmailQueueJob](repeatInterval = 1.minute),
		SimpleTriggerJob[ProcessNotificationListenersJob](repeatInterval = 30.seconds),

		SimpleTriggerJob[ProcessJobQueueJob](repeatInterval = 10.seconds),

		SimpleTriggerJob[UpdateCheckpointTotalsJob](repeatInterval = 10.seconds),

		SimpleTriggerJob[ProcessTurnitinLtiQueueJob](repeatInterval = 20.seconds),
		SimpleTriggerJob[ProcessUrkundQueueJob](repeatInterval = 10.seconds)

		// Migration now complete, don't need this any more
		// SimpleTriggerJob[ObjectStorageMigrationJob](repeatInterval = 1.minute)
	)

	val scheduledSitsJobs: Seq[ScheduledJob[_, _ <: Trigger]] = Seq(
		// SITS exports
		SimpleTriggerJob[ExportAttendanceToSitsJob](repeatInterval = 5.minutes),
		SimpleTriggerJob[ExportFeedbackToSitsJob](repeatInterval = 5.minutes),
		SimpleTriggerJob[ExportYearMarksToSitsJob](repeatInterval = 5.minutes)
	)
}

@Configuration
class JobFactoryConfiguration {
	@Bean def jobFactory(): AutowiringSpringBeanJobFactory = new AutowiringSpringBeanJobFactory
}

@Configuration
@Profile(Array("dev", "production", "scheduling", "sandbox"))
class SchedulingConfiguration {

	@Autowired var transactionManager: PlatformTransactionManager = _
	@Qualifier("dataSource") @Autowired var dataSource: DataSource = _
	@Autowired var jobFactory: SpringBeanJobFactory = _

	@Autowired var env: Environment = _
	@Autowired var maintenanceModeService: MaintenanceModeService = _

	@Value("${toplevel.url}") var toplevelUrl: String = _

	private def scheduler(scheduledJobs: Seq[ScheduledJob[_, _ <: Trigger]]): FactoryBean[Scheduler] = {
		// If we're deploying a change that means an existing trigger is no longer referenced, clear the scheduler
		val triggerNames: Seq[String] =
			new JdbcTemplate(dataSource).queryAndMap("select trigger_name from qrtz_triggers") {
				case (rs, _) => rs.getString("trigger_name")
			}

		val jobs = Seq(scheduledJobs, SchedulingConfiguration.unscheduledJobs).flatten
		val jobNames = jobs.map { _.name }

		// Clear the scheduler if there is a trigger that we no longer want to run
		val clearScheduler = !triggerNames.forall(jobNames.contains)

		val factory = new SchedulerFactoryBean() {
			override def createScheduler(schedulerFactory: SchedulerFactory, schedulerName: String): Scheduler = {
				val scheduler = super.createScheduler(schedulerFactory, schedulerName)

				if (clearScheduler) {
					scheduler.clear()
				}

				scheduler
			}
		}

		factory.setConfigLocation(new ClassPathResource("/quartz.properties"))
		factory.setStartupDelay(10)
		factory.setDataSource(dataSource)
		factory.setTransactionManager(transactionManager)
		factory.setSchedulerName(Uri.parse(toplevelUrl).getAuthority)
		factory.setOverwriteExistingJobs(true)

		// We only auto-startup on the scheduler, and only if we're not in maintenance mode. This allows us
		// to wire a scheduler on nodes that wouldn't normally get one and use it to schedule jobs. Neat!
		factory.setAutoStartup(env.acceptsProfiles("scheduling") && !maintenanceModeService.enabled)

		if (!env.acceptsProfiles("scheduling")) {
			factory.setQuartzProperties(new Properties() {{
				setProperty("org.quartz.jobStore.isClustered", "false")
			}})
		}

		factory.setApplicationContextSchedulerContextKey("applicationContext")
		factory.setJobFactory(jobFactory)

		factory.setJobDetails(jobs.map { _.jobDetail }: _*)
		factory.setTriggers(scheduledJobs.map { _.trigger }: _*)

		factory
	}

	@Bean
	@Profile(Array("dev", "production"))
	def schedulerWithSitsExports(): FactoryBean[Scheduler] = {
		scheduler(SchedulingConfiguration.scheduledJobs ++ SchedulingConfiguration.scheduledSitsJobs)
	}

	@Bean
	@Profile(Array("sandbox"))
	def schedulerNoSitsExports(): FactoryBean[Scheduler] = {
		scheduler(SchedulingConfiguration.scheduledJobs)
	}

}

@Configuration
@Profile(Array("test"))
class TestSchedulingConfiguration {
	@Bean def scheduler(): FactoryBean[Scheduler] = {
		new SchedulerFactoryBean
	}
}

@Component
@Profile(Array("scheduling"))
class SchedulingMaintenanceModeObserver extends InitializingBean {

	@Autowired var maintenanceModeService: MaintenanceModeService = _
	@Autowired var scheduler: Scheduler = _

	override def afterPropertiesSet(): Unit = {
		// listen for maintenance mode changes
		maintenanceModeService.changingState.observe { enabled =>
			if (enabled) scheduler.standby()
			else scheduler.start()
		}
	}
}

trait AutowiredJobBean extends QuartzJobBean {

	protected var features: Features = Wire[Features]
	protected var exceptionResolver: ExceptionResolver = Wire[ExceptionResolver]

}
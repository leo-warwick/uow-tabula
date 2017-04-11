package uk.ac.warwick.tabula.services.healthchecks

import humanize.Humanize._
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{AuditEvent, Department}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryService

import scala.concurrent.Await
import scala.concurrent.duration._

abstract class AbstractImportStatusHealthcheck extends ServiceHealthcheckProvider {

	def WarningThreshold: Duration
	def ErrorThreshold: Duration
	def HealthcheckName: String

	/**
		* Fetch a list of audit events, most recent first, relating to this import
		*/
	protected def auditEvents: Seq[AuditEvent]

	protected def getServiceHealthCheck(imports: Seq[AuditEvent]): ServiceHealthcheck = {
		// Get the last one that's successful
		val lastSuccessful = imports.find { event => !event.hadError && !event.isIncomplete }

		// Do we have a current running import?
		val isRunning = imports.headOption.filter(_.isIncomplete)

		// Did the last import fail
		val lastFailed = imports.find(!_.isIncomplete).filter(_.hadError)

		val status =
			if (!lastSuccessful.exists(_.eventDate.plusMillis(ErrorThreshold.toMillis.toInt).isAfterNow))
				ServiceHealthcheck.Status.Error
			else if (!lastSuccessful.exists(_.eventDate.plusMillis(WarningThreshold.toMillis.toInt).isAfterNow) || lastFailed.nonEmpty)
				ServiceHealthcheck.Status.Warning
			else
				ServiceHealthcheck.Status.Okay

		val successMessage =
			lastSuccessful.map { event => s"Last successful import ${naturalTime(event.eventDate.toDate)}" }

		val runningMessage =
			isRunning.map { event => s"import started ${naturalTime(event.eventDate.toDate)}" }

		val failedMessage =
			lastFailed.map { event => s"last import failed ${naturalTime(event.eventDate.toDate)}" }

		val message = Seq(
			successMessage.orElse(Some("No successful import found")),
			runningMessage,
			failedMessage
		).flatten.mkString(", ")

		val lastSuccessfulHoursAgo: Double =
			lastSuccessful.map { event =>
				val d = new org.joda.time.Duration(event.eventDate, DateTime.now)
				d.toStandardSeconds.getSeconds / 3600.0
			}.getOrElse(0)

		ServiceHealthcheck(
			name = HealthcheckName,
			status = status,
			testedAt = DateTime.now,
			message = message,
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("last_successful_hours", lastSuccessfulHoursAgo, WarningThreshold.toHours, ErrorThreshold.toHours)
			)
		)
	}

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		val imports = auditEvents

		update(getServiceHealthCheck(imports))
	}

}

@Component
@Profile(Array("scheduling"))
class AcademicDataImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 2 days, critical if no import for 3 days
	override val WarningThreshold: FiniteDuration = 2.days
	override val ErrorThreshold: FiniteDuration = 3.days
	override val HealthcheckName = "import-academic"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportAcademicInformation", 0, 50), 1.minute)
	}

}

@Component
@Profile(Array("scheduling"))
class ProfileImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 3 days, critical for 4 days
	override val WarningThreshold: FiniteDuration = 3.days
	override val ErrorThreshold: FiniteDuration = 4.days
	override val HealthcheckName = "import-profiles"

	lazy val moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportProfiles", 0, 1000), 1.minute)
	}

	private def checkDepartment(imports: Seq[AuditEvent], department: Department): (Department, ServiceHealthcheck) = {
		val thisDepartmentImports = imports.filter(event =>
			event.data == "{\"deptCode\":\"%s\"}".format(department.code)
				// legacy imports
				|| event.data == "{\"deptCode\":null}" || event.data == "{\"deptCode\":\"\"}"
		)
		(department, getServiceHealthCheck(thisDepartmentImports))
	}

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	override def run(): Unit = transactional(readOnly = true) {
		val allDepartments = moduleAndDepartmentService.allDepartments
		val imports = auditEvents

		val healthchecks = allDepartments.map(department => checkDepartment(imports, department))

		val (department, healthcheckToUpdate): (Department, ServiceHealthcheck) = {
			val errors = healthchecks.filter { case (_, check) => check.status == ServiceHealthcheck.Status.Error }
			val warnings = healthchecks.filter { case (_, check) => check.status == ServiceHealthcheck.Status.Warning }

			// Show oldest import
			def sorted(departmentAndChecks: Seq[(Department, ServiceHealthcheck)]): (Department, ServiceHealthcheck) = {
				departmentAndChecks.sortBy { case (_, check) => check.performanceData.head.value match {
					case lastSuccessfulHoursAgo: Double => lastSuccessfulHoursAgo
					case _ => 0
				}}.last
			}

			if (errors.nonEmpty) {
				sorted(errors)
			} else if (warnings.nonEmpty) {
				sorted(warnings)
			} else {
				sorted(healthchecks)
			}
		}

		update(ServiceHealthcheck(
			name = healthcheckToUpdate.name,
			status = healthcheckToUpdate.status,
			testedAt = healthcheckToUpdate.testedAt,
			message = Seq(healthcheckToUpdate.message, s"oldest department ${department.code}").mkString(", "),
			performanceData = healthcheckToUpdate.performanceData
		))
	}

}

@Component
@Profile(Array("scheduling"))
class AssignmentImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 3 days, critical for 4 days
	override val WarningThreshold: FiniteDuration = 3.days
	override val ErrorThreshold: FiniteDuration = 4.days
	override val HealthcheckName = "import-assignments"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportAssignments", 0, 50), 1.minute)
	}

}

@Component
@Profile(Array("scheduling"))
class ModuleListImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 3 days, critical for 4 days
	override val WarningThreshold: FiniteDuration = 3.days
	override val ErrorThreshold: FiniteDuration = 4.days
	override val HealthcheckName = "import-module-lists"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportModuleLists", 0, 50), 1.minute)
	}

}

@Component
@Profile(Array("scheduling"))
class RouteRuleImportStatusHealthcheck extends AbstractImportStatusHealthcheck {

	// Warn if no successful import for 3 days, critical for 4 days
	override val WarningThreshold: FiniteDuration = 3.days
	override val ErrorThreshold: FiniteDuration = 4.days
	override val HealthcheckName = "import-route-rules"

	override protected def auditEvents: Seq[AuditEvent] = {
		val queryService = Wire[AuditEventQueryService]
		Await.result(queryService.query("eventType:ImportRouteRules", 0, 50), 1.minute)
	}

}
package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportAttendanceToSitsServiceComponent, ExportAttendanceToSitsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

object ExportAttendanceToSitsCommand {
	def apply() = new ExportAttendanceToSitsCommand
		with ComposableCommand[Seq[MonitoringPointReport]]
		with ExportAttendanceToSitsCommandPermissions
		with ExportAttendanceToSitsCommandDescription
		with AutowiringAttendanceMonitoringServiceComponent
		with AutowiringExportAttendanceToSitsServiceComponent
		with AutowiringFeaturesComponent
}

class ExportAttendanceToSitsCommand extends CommandInternal[Seq[MonitoringPointReport]] with Logging {

	self: AttendanceMonitoringServiceComponent with ExportAttendanceToSitsServiceComponent with FeaturesComponent =>

	override def applyInternal(): Seq[MonitoringPointReport] = transactional() {

		// check reporting to sits feature is on -- with Features...
		if (features.attendanceMonitoringReport) {

			// get the reports from service
			val unreportedReports = attendanceMonitoringService.listUnreportedReports

			// for each student
			unreportedReports.groupBy(_.student).flatMap{case(student, reportList) =>
				// group reports by academic year
				val reportsByAcademicYear = reportList.groupBy(_.academicYear)
				// for each academic year in order
				reportsByAcademicYear.keys.toSeq.sortBy(_.startYear).flatMap{academicYear =>
					val reportsInAcademicYear = reportsByAcademicYear(academicYear)
					// push a report for each term in order (if a report exists)
					TermService.orderedTermNames.flatMap(term => {
						reportsInAcademicYear.find(_.monitoringPeriod == term) match {
							case None => None
							case Some(report: MonitoringPointReport) =>
								val result = exportAttendanceToSitsService.exportToSits(report)
								if (result) {
										attendanceMonitoringService.markReportAsPushed(report)
										logger.info(s"Reported ${
											report.missed
										} missed points for ${
											report.student.universityId
										} for ${
											report.monitoringPeriod
										} ${
											report.academicYear.toString
										}")
										Option(report)
									}
								else {
									logger.error(s"Could not push monitoring report to SITS for ${report.student.universityId}")
									None
								}
						}
					})
				}
			}.toSeq

		} else {
			Seq()
		}
	}

}

trait ExportAttendanceToSitsCommandPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Export)
	}
}

trait ExportAttendanceToSitsCommandDescription extends Describable[Seq[MonitoringPointReport]] {
	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[MonitoringPointReport]) {
		d.property("reports exported", result.size)
	}
}
package uk.ac.warwick.tabula.commands.reports.smallgroups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.SmallGroupEventReportData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}

object SmallGroupEventsReportCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new SmallGroupEventsReportCommandInternal(department, academicYear)
			with AutowiringSmallGroupServiceComponent
			with ComposableCommand[Seq[SmallGroupEventReportData]]
			with ReportPermissions
			with ReportCommandState
			with ReadOnly with Unaudited
}


class SmallGroupEventsReportCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[SmallGroupEventReportData]] {

	self: SmallGroupServiceComponent =>

	override def applyInternal(): Seq[SmallGroupEventReportData] = {
		smallGroupService.listSmallGroupEventsForReport(department, academicYear)
	}

}

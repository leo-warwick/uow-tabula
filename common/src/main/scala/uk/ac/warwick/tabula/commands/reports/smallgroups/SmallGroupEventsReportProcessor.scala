package uk.ac.warwick.tabula.commands.reports.smallgroups

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.data.SmallGroupEventReportData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConverters._
import scala.collection.mutable

object SmallGroupEventsReportProcessor {
	def apply(department: Department, academicYear: AcademicYear) =
		new SmallGroupEventsReportProcessorInternal(department, academicYear)
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Seq[SmallGroupEventReportData]]
			with ReportPermissions
			with SmallGroupEventsReportProcessorState
			with ReadOnly with Unaudited {
			override lazy val eventName: String = "SmallGroupEventsReportProcessor"
		}
}

class SmallGroupEventsReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[SmallGroupEventReportData]] with TaskBenchmarking {

	self: SmallGroupEventsReportProcessorState with TermServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): mutable.Buffer[SmallGroupEventReportData] = {
		events.asScala.map(properties =>
			SmallGroupEventReportData(
				departmentName = properties.get("departmentName"),
				eventName = properties.get("eventName"),
				moduleTitle = properties.get("moduleTitle"),
				day = properties.get("day"),
				start = properties.get("start"),
				finish = properties.get("finish"),
				location = properties.get("location"),
				size = properties.get("size").toInt,
				weeks = properties.get("weeks"),
				staff = properties.get("staff")
			)
		)
	}

}

trait SmallGroupEventsReportProcessorState extends ReportCommandState {
	var events: JList[JMap[String, String]] = JArrayList()
}

package uk.ac.warwick.tabula.web.controllers.reports.smallgroups

import java.io.StringWriter

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.reports.smallgroups._
import uk.ac.warwick.tabula.data.SmallGroupEventReportData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.reports.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, JsonHelper}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.xml.Elem

@Controller
@RequestMapping(Array("/reports/{department}/{academicYear}/groups/events"))
class SmallGroupEventsReportController extends ReportsController
	with DepartmentScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AcademicYearScopedController with AutowiringMaintenanceModeServiceComponent {

	type SmallGroupEventsReportProcessor = Appliable[Seq[SmallGroupEventReportData]] with SmallGroupEventsReportProcessorState

	override val departmentPermission: Permission = Permissions.Department.Reports

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		SmallGroupEventsReportCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		SmallGroupEventsReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(
		@ModelAttribute("command") cmd: Appliable[Seq[SmallGroupEventReportData]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav(s"reports/smallgroups/events")
			.crumbs(ReportsBreadcrumbs.SmallGroups.Home(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.SmallGroups.events(department, year)):_*)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[Seq[SmallGroupEventReportData]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val result = cmd.apply()
		Mav(new JSONView(Map("events" -> result.map(data => Map(
			"departmentName" -> data.departmentName,
			"eventName" -> data.eventName,
			"moduleTitle" -> data.moduleTitle,
			"day" -> data.day,
			"start" -> data.start,
			"finish" -> data.finish,
			"location" -> data.location,
			"size" -> data.size,
			"weeks" -> data.weeks,
			"staff" -> data.staff
		)))))
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: SmallGroupEventsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): CSVView = {
		val processorResult = getProcessorResult(processor, data)

		val writer = new StringWriter
		val csvBuilder = new SmallGroupEventsReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"events-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: SmallGroupEventsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): ExcelView = {
		val processorResult = getProcessorResult(processor, data)

		val workbook = new SmallGroupEventsReportExporter(processorResult, department).toXLSX

		new ExcelView(s"events-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: SmallGroupEventsReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): Elem = {
		val processorResult = getProcessorResult(processor, data)

		new SmallGroupEventsReportExporter(processorResult, department).toXML
	}

	private def getProcessorResult(processor: SmallGroupEventsReportProcessor, data: String): Seq[SmallGroupEventReportData] = {
		val request = JsonHelper.fromJson[SmallGroupEventsReportRequest](data)
		request.copyTo(processor)
		processor.apply()
	}

}

class SmallGroupEventsReportRequest extends Serializable {

	var events: JList[JMap[String, String]] = JArrayList()

	def copyTo(state: SmallGroupEventsReportProcessorState) {
		state.events = events
	}
}

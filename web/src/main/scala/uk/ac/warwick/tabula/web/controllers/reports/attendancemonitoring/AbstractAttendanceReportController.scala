package uk.ac.warwick.tabula.web.controllers.reports.attendancemonitoring

import java.io.StringWriter

import freemarker.template.{Configuration, DefaultObjectWrapper}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.reports.attendancemonitoring.AllAttendanceReportCommand.AllAttendanceReportCommandResult
import uk.ac.warwick.tabula.commands.reports.attendancemonitoring._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.{IntervalFormatter, LazyMaps}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.reports.{ReportsBreadcrumbs, ReportsController}
import uk.ac.warwick.tabula.web.views.{CSVView, ExcelView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, JsonHelper}
import uk.ac.warwick.util.csv.GoodCsvDocument

import scala.collection.JavaConverters._
import scala.xml.Elem

abstract class AbstractAttendanceReportController extends ReportsController
	with DepartmentScopedController with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.Reports

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	def command(department: Department, academicYear: AcademicYear): Appliable[AllAttendanceReportCommandResult]

	val pageRenderPath: String
	val filePrefix: String
	def urlGeneratorFactory(department: Department): (AcademicYear) => String

	type AttendanceReportProcessor = Appliable[AttendanceReportProcessorResult] with AttendanceReportProcessorState

	@ModelAttribute("processor")
	def processor(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AttendanceReportProcessor(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def page(
		@ModelAttribute("command") cmd: Appliable[AllAttendanceReportCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav(s"reports/attendancemonitoring/$pageRenderPath")
			.crumbs(ReportsBreadcrumbs.Attendance.Home(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(urlGeneratorFactory(department)): _*)
	}

	@RequestMapping(method = Array(POST))
	def apply(
		@ModelAttribute("command") cmd: Appliable[AllAttendanceReportCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val result = cmd.apply()
		val allStudents: Seq[Map[String, String]] = result.keys.toSeq.sortBy(s => (s.lastName, s.firstName)).map(studentData =>
			 Map(
         "firstName" -> studentData.firstName,
         "lastName" -> studentData.lastName,
         "userId" -> studentData.userId,
         "universityId" -> studentData.universityId,
				 "yearOfStudy" -> studentData.yearOfStudy,
         "sprCode" -> studentData.sprCode,
         "route" -> studentData.routeCode
       )
		)
		val intervalFormatter = new IntervalFormatter
		val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_0)
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		val allPoints: Seq[Map[String, String]] = result.values.flatMap(_.keySet).toSeq.distinct.sortBy(p => (p.startDate, p.endDate)).map(point =>
			Map(
				"id" -> point.id,
				"name" -> point.name,
				"startDate" -> point.startDate.toDateTimeAtStartOfDay.getMillis.toString,
				"endDate" -> point.endDate.toDateTimeAtStartOfDay.getMillis.toString,
				"intervalString" -> intervalFormatter.exec(JList(wrapper.wrap(point.startDate), wrapper.wrap(point.endDate))).asInstanceOf[String],
				"late" -> point.endDate.toDateTimeAtStartOfDay.plusDays(1).isBeforeNow.toString
			)
		)
		Mav(new JSONView(Map(
			"attendance" -> result.map{case(studentData, pointMap) =>
				studentData.universityId -> pointMap.map{case(point, state) =>
					point.id -> Option(state).map(_.dbValue).orNull
				}
			},
			"students" -> allStudents,
			"points" -> allPoints
		)))
	}

	@RequestMapping(method = Array(POST), value = Array("/download.csv"))
	def csv(
		@ModelAttribute("processor") processor: AttendanceReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): CSVView = {
		val processorResult = getProcessorResult(processor, data)

		val writer = new StringWriter
		val csvBuilder = new AttendanceReportExporter(processorResult, department)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		processorResult.attendance.keys.foreach(item => doc.addLine(item))
		doc.write(writer)

		new CSVView(s"$filePrefix-${department.code}.csv", writer.toString)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xlsx"))
	def xlsx(
		@ModelAttribute("processor") processor: AttendanceReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): ExcelView = {
		val processorResult = getProcessorResult(processor, data)

		val workbook = new AttendanceReportExporter(processorResult, department).toXLSX

		new ExcelView(s"$filePrefix-${department.code}.xlsx", workbook)
	}

	@RequestMapping(method = Array(POST), value = Array("/download.xml"))
	def xml(
		@ModelAttribute("processor") processor: AttendanceReportProcessor,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam data: String
	): Elem = {
		val processorResult = getProcessorResult(processor, data)

		new AttendanceReportExporter(processorResult, department).toXML
	}

	private def getProcessorResult(processor: AttendanceReportProcessor, data: String): AttendanceReportProcessorResult = {
		val request = JsonHelper.fromJson[AttendanceReportRequest](data)
		request.copyTo(processor)
		processor.apply()
	}

}

class AttendanceReportRequest extends Serializable {

	var attendance: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var students: JList[JMap[String, String]] = JArrayList()

	var points: JList[JMap[String, String]] = JArrayList()

	def copyTo(state: AttendanceReportProcessorState) {
		state.attendance = attendance

		state.students = students

		state.points = points
	}
}

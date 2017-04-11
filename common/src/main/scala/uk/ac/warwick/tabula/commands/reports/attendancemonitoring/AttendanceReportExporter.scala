package uk.ac.warwick.tabula.commands.reports.attendancemonitoring

import freemarker.template.{Configuration, DefaultObjectWrapper}
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState.{MissedUnauthorised, NotRecorded}
import uk.ac.warwick.tabula.helpers.IntervalFormatter
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class AttendanceReportExporter(val processorResult: AttendanceReportProcessorResult, val department: Department)
	extends CSVLineWriter[AttendanceMonitoringStudentData] {

	val intervalFormatter = new IntervalFormatter
	val wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_0)
	val isoFormatter = DateFormats.IsoDateTime

	val result: Map[AttendanceMonitoringStudentData, Map[PointData, AttendanceState]] = processorResult.attendance
	val students: Seq[AttendanceMonitoringStudentData] = processorResult.students
	val points: Seq[PointData] = processorResult.points

	val headers: Seq[String] = Seq("First name","Last name","University ID", "Route", "Year of study", "SPR code") ++
		points.map(p => s"${p.name} (${intervalFormatter.exec(JList(
			wrapper.wrap(p.startDate.toDate),
			wrapper.wrap(p.endDate.toDate)
		)).asInstanceOf[String].replaceAll("<sup>","").replaceAll("</sup>","")})") ++
		Seq("Unrecorded","Missed (unauthorised)")

	val unrecordedIndex: Int = headers.size - 2
	val missedIndex: Int = headers.size - 1

	override def getNoOfColumns(o: AttendanceMonitoringStudentData): Int = headers.size

	override def getColumn(studentData: AttendanceMonitoringStudentData, pointIndex: Int): String = {
		pointIndex match {
			case 0 =>
				studentData.firstName
			case 1 =>
				studentData.lastName
			case 2 =>
				studentData.universityId
			case 3 =>
				studentData.routeCode
			case 4 =>
				studentData.yearOfStudy
			case 5 =>
				studentData.sprCode
			case index if index == unrecordedIndex =>
				result(studentData).map { case (point, state) => state}.count(_ == NotRecorded).toString
			case index if index == missedIndex =>
				result(studentData).map { case (point, state) => state}.count(_ == MissedUnauthorised).toString
			case _ =>
				val thisPoint = points(pointIndex - 6)
				result(studentData).get(thisPoint).map{
					case state if state == NotRecorded =>
						if (thisPoint.isLate)
							"Late"
						else
							state.description
					case state =>
						state.description
				}.getOrElse("n/a")
		}
	}

	def toXLSX: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(workbook)

		result.keys.foreach(addRow(sheet))

		(0 to headers.size) map sheet.autoSizeColumn
		workbook
	}

	private def generateNewSheet(workbook: XSSFWorkbook) = {
		val sheet = workbook.createSheet(department.name)

		// add header row
		val headerRow = sheet.createRow(0)
		headers.zipWithIndex foreach {
			case (header, index) => headerRow.createCell(index).setCellValue(header)
		}
		sheet
	}

	private def addRow(sheet: XSSFSheet)(studentData: AttendanceMonitoringStudentData) {
		val plainCellStyle = {
			val cs = sheet.getWorkbook.createCellStyle()
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("@"))
			cs
		}

		val row = sheet.createRow(sheet.getLastRowNum + 1)
		headers.zipWithIndex foreach { case (_, index) =>
			val cell = row.createCell(index)

			if (index == 2) {
				// University IDs have leading zeros and Excel would normally remove them.
				// Set a manual data format to remove this possibility
				cell.setCellStyle(plainCellStyle)
			}

			cell.setCellValue(getColumn(studentData, index))
		}
	}

	def toXML: Elem = {
		<result>
			<attendance>
				{ result.map{case(studentData, pointMap) =>
					<student universityid={studentData.universityId}>
						{ pointMap.map{case(point, state) =>
							<point id={point.id}>
								{ state }
							</point>
						}}
					</student>
				}}
			</attendance>

			<students>
				{ students.map(studentData =>
					<student
						firstname={studentData.firstName}
						lastname={studentData.lastName}
						universityid={studentData.universityId}
						route={studentData.routeCode}
						year={studentData.yearOfStudy}
						spr={studentData.sprCode}
					/>
				)}
			</students>
			<points>
					{ points.map(point =>
						<point
							id={point.id}
							name={point.name}
							startdate={isoFormatter.print(point.startDate.toDateTimeAtStartOfDay)}
							enddate={isoFormatter.print(point.endDate.toDateTimeAtStartOfDay)}
						/>
				)}
				</points>
		</result>
	}
}

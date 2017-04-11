package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.apache.poi.hssf.usermodel.HSSFDataFormat
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class SmallGroupsByModuleReportExporter(val processorResult: SmallGroupsByModuleReportProcessorResult, val department: Department)
	extends CSVLineWriter[AttendanceMonitoringStudentData] {

	val counts: Map[AttendanceMonitoringStudentData, Map[ModuleData, Int]] = processorResult.counts
	val students: Seq[AttendanceMonitoringStudentData] = processorResult.students
	val modules: Seq[ModuleData] = processorResult.modules

	val headers: Seq[String] = Seq("First name","Last name","University ID", "Route", "Year of study", "SPR code") ++
		modules.map(m => s"${m.code} ${m.name}")

	override def getNoOfColumns(o: AttendanceMonitoringStudentData): Int = headers.size

	override def getColumn(studentData: AttendanceMonitoringStudentData, moduleIndex: Int): String = {
		moduleIndex match {
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
			case _ =>
				val thisModule = modules(moduleIndex - 6)
				counts.get(studentData).flatMap(_.get(thisModule).map{_.toString}).getOrElse("n/a")
		}
	}

	def toXLSX: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(workbook)

		students.foreach(addRow(sheet))

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
			<counts>
				{ counts.map{case(studentData, moduleMap) =>
					<student universityid={studentData.universityId}>
						{ moduleMap.map{case(module, count) =>
							<module id={module.id}>
								{ count }
							</module>
						}}
					</student>
				}}
			</counts>

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
			<modules>
					{ modules.map(module =>
						<module
							id={module.id}
							code={module.code}
							name={module.name}
						/>
				)}
				</modules>
		</result>
	}
}

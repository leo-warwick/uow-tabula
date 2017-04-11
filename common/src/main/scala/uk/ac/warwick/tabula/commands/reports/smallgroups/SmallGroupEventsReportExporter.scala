package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}
import uk.ac.warwick.tabula.data.SmallGroupEventReportData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.util.csv.CSVLineWriter

import scala.xml.Elem

class SmallGroupEventsReportExporter(val processorResult: Seq[SmallGroupEventReportData], val department: Department)
	extends CSVLineWriter[SmallGroupEventReportData] {

	val headers = Seq(
		"Department name",
		"Event name",
		"Module title",
		"Day",
		"Start",
		"Finish",
		"Location",
		"Size",
		"Weeks",
		"Staff"
	)

	override def getNoOfColumns(o: SmallGroupEventReportData): Int = headers.size

	override def getColumn(data: SmallGroupEventReportData, eventIndex: Int): String = {
		eventIndex match {
			case 0 =>
				data.departmentName
			case 1 =>
				data.eventName
			case 2 =>
				data.moduleTitle
			case 3 =>
				data.day
			case 4 =>
				data.start
			case 5 =>
				data.finish
			case 6 =>
				data.location
			case 7 =>
				data.size.toString
			case 8 =>
				data.weeks
			case 9 =>
				data.staff
		}
	}

	def toXLSX: XSSFWorkbook = {
		val workbook = new XSSFWorkbook()
		val sheet = generateNewSheet(workbook)

		processorResult.foreach(addRow(sheet))

		(0 to headers.size) foreach sheet.autoSizeColumn
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

	private def addRow(sheet: XSSFSheet)(data: SmallGroupEventReportData) {
		val row = sheet.createRow(sheet.getLastRowNum + 1)
		headers.zipWithIndex foreach { case (_, index) =>
			val cell = row.createCell(index)
			cell.setCellValue(getColumn(data, index))
		}
	}

	def toXML: Elem = {
		<events>
				{ processorResult.map(data =>
					<event
						departmentName={data.departmentName}
						eventName={data.eventName}
						moduleTitle={data.moduleTitle}
						day={data.day}
						start={data.start}
						finish={data.finish}
						location={data.location}
						size={data.size.toString}
						weeks={data.weeks}
						staff={data.staff}
					/>
			)}
		</events>
	}
}

package uk.ac.warwick.tabula.helpers

import org.apache.poi.xssf.usermodel._
import org.apache.poi.ss.usermodel.{Cell, Font}
import uk.ac.warwick.tabula.DateFormats
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.tabula.data.model.Department
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.InputStream

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.model.StylesTable
import org.xml.sax.helpers.XMLReaderFactory
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.hssf.util.CellReference

import scala.collection.JavaConverters._
import org.xml.sax.{InputSource, XMLReader}

import scala.collection.mutable

trait SpreadsheetHelpers {
	def parseXSSFExcelFile(file: InputStream, simpleHeaders: Boolean = true): Seq[Map[String, String]]
}

object SpreadsheetHelpers extends SpreadsheetHelpers {

	val MaxDepartmentNameLength: Int = 31 - 11

	// trim the department name down to 20 characters. Excel sheet names must be 31 chars or less so
	def trimmedDeptName(department: Department): String = {
		if (department.name.length > MaxDepartmentNameLength)
			department.name.substring(0, MaxDepartmentNameLength)
		else
			department.name
	}

	// replace unsafe characters with spaces
	def safeDeptName(department: Department): String = WorkbookUtil.createSafeSheetName(trimmedDeptName(department))


	def dateCellStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat(DateFormats.CSVDatePattern))
		cellStyle
	}

	def percentageCellStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"))
		cellStyle
	}

	def headerStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		val font = workbook.createFont()
		font.setBoldweight(Font.BOLDWEIGHT_BOLD)
		cellStyle.setFont(font)
		cellStyle
	}

	def getNextCellNum(row: XSSFRow):Short = if(row.getLastCellNum == -1) 0 else row.getLastCellNum

	def addCell(row: XSSFRow, cellType: Int): XSSFCell = {
		val cell = row.createCell(getNextCellNum(row))
		cell.setCellType(cellType)
		cell
	}

	def addStringCell(value: String, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellValue(value)
	}

	def addStringCell(value: String, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addNumericCell(value: Double, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellValue(value)
	}

	def addNumericCell(value: Double, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addDateCell(value: DateTime, row: XSSFRow, style: XSSFCellStyle) {
		addDateCell(Option(value).map { _.toLocalDate }.orNull, row, style)
	}

	def addDateCell(value: LocalDate, row: XSSFRow, style: XSSFCellStyle) {
		if (value != null) {
			val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
			cell.setCellStyle(style)
			cell.setCellValue(Option(value).map { _.toDate }.orNull)
		} else addCell(row, Cell.CELL_TYPE_BLANK)
	}

	def addPercentageCell(num:Double, total:Double, row: XSSFRow, workbook: XSSFWorkbook) {
		if (total == 0)
			addStringCell("N/A", row)
		else
			addNumericCell(num / total, row, percentageCellStyle(workbook))
	}

	def formatWorksheet(sheet: XSSFSheet, cols: Int) {
		(0 to cols).map(sheet.autoSizeColumn)
	}

	/**
	 * If simpleHeaders is set to true, the parser will:
	 *
	 * - lower-case all headers
	 * - trim the header and remove all non-ascii characters
	 */
	def parseXSSFExcelFile(file: InputStream, simpleHeaders: Boolean = true): Seq[Map[String, String]] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable

		reader.getSheetsData.asScala.toSeq.flatMap { sheet =>
			val handler = new XslxParser(styles, sst, simpleHeaders)
			val parser = handler.fetchSheetParser

			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()

			handler.rows.toSeq
		}
	}
}

class XslxParser(val styles: StylesTable, val sst: ReadOnlySharedStringsTable, val simpleHeaders: Boolean = true)
	extends SheetContentsHandler with Logging {

	var isParsingHeader = true // flag to parse the first row for column headers
	var columnMap: mutable.Map[Short, String] = scala.collection.mutable.Map[Short, String]()
	val xssfHandler = new XSSFSheetXMLHandler(styles, sst, this, false)

	var rows: scala.collection.mutable.MutableList[Map[String, String]] = scala.collection.mutable.MutableList()
	var currentRow: mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()

	def fetchSheetParser: XMLReader = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		parser.setContentHandler(xssfHandler)
		parser
	}

	// implement SheetContentsHandler
	override def headerFooter(text: String, isHeader: Boolean, tagName: String): Unit = {
		// don't care about handling this, but required for interface
	}

	override def startRow(row: Int): Unit = {
		logger.debug("startRow: " + row.toString)
		isParsingHeader = row == 0
		currentRow = scala.collection.mutable.Map[String, String]()
	}

	def formatHeader(rawValue: String): String = {
		if (simpleHeaders) rawValue.trim().toLowerCase.replaceAll("[^\\x00-\\x7F]", "")
		else rawValue
	}

	override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
		val col = new CellReference(cellReference).getCol

		if (isParsingHeader) columnMap(col) = formatHeader(formattedValue)
		else if (columnMap.contains(col)) currentRow(columnMap(col)) = formattedValue
	}

	override def endRow(row: Int): Unit = {
		if (!isParsingHeader && currentRow.nonEmpty)
			rows += currentRow.toMap
	}
}
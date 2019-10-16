package uk.ac.warwick.tabula.helpers

import java.io.InputStream

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.{CellReference, WorkbookUtil}
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader, XSSFSheetXMLHandler}
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.streaming.{SXSSFDrawing, SXSSFSheet, SXSSFWorkbook}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.joda.time.{DateTime, LocalDate}
import org.xml.sax.{InputSource, XMLReader}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.Department

import scala.collection.mutable

trait SpreadsheetHelpers {
  def parseXSSFExcelFile(file: InputStream, simpleHeaders: Boolean = true): Seq[ParsedRow]
  def parseXSSFExcelFileWithSheetMetadata(file: InputStream, simpleHeaders: Boolean = true): Seq[ParsedSheet]
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


  def dateCellStyle(workbook: SXSSFWorkbook): CellStyle = {
    val cellStyle = workbook.createCellStyle
    cellStyle.setDataFormat(workbook.createDataFormat().getFormat(DateFormats.CSVDatePattern))
    cellStyle
  }

  def percentageCellStyle(workbook: SXSSFWorkbook): CellStyle = {
    val cellStyle = workbook.createCellStyle
    cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"))
    cellStyle
  }

  def headerStyle(workbook: SXSSFWorkbook): CellStyle = {
    val cellStyle = workbook.createCellStyle
    val font = workbook.createFont()
    font.setBold(true)
    cellStyle.setFont(font)
    cellStyle
  }

  def getNextCellNum(row: Row): Short = if (row.getLastCellNum == -1) 0 else row.getLastCellNum

  def addCell(row: Row, cellType: CellType): Cell = row.createCell(getNextCellNum(row), cellType)

  def addStringCell(value: String, row: Row) {
    val cell = addCell(row, CellType.STRING)
    cell.setCellValue(value)
  }

  def addStringCell(value: String, row: Row, style: CellStyle) {
    val cell = addCell(row, CellType.STRING)
    cell.setCellStyle(style)
    cell.setCellValue(value)
  }

  def addNumericCell(value: Double, row: Row) {
    val cell = addCell(row, CellType.NUMERIC)
    cell.setCellValue(value)
  }

  def addNumericCell(value: Double, row: Row, style: CellStyle) {
    val cell = addCell(row, CellType.NUMERIC)
    cell.setCellStyle(style)
    cell.setCellValue(value)
  }

  def addDateCell(value: DateTime, row: Row, style: CellStyle) {
    addDateCell(Option(value).map(_.toLocalDate).orNull, row, style)
  }

  def addDateCell(value: LocalDate, row: Row, style: CellStyle) {
    if (value != null) {
      val cell = addCell(row, CellType.NUMERIC)
      cell.setCellStyle(style)
      cell.setCellValue(Option(value).map(_.toDate).orNull)
    } else addCell(row, CellType.BLANK)
  }

  def addPercentageCell(num: Double, total: Double, row: Row, workbook: SXSSFWorkbook) {
    if (total == 0)
      addStringCell("N/A", row)
    else
      addNumericCell(num / total, row, percentageCellStyle(workbook))
  }

  def formatWorksheet(sheet: Sheet, cols: Int) {
    (0 to cols).foreach(sheet.autoSizeColumn)
  }

  /**
    * If simpleHeaders is set to true, the parser will:
    *
    * - lower-case all headers
    * - trim the header and remove all non-ascii characters
    */
  override def parseXSSFExcelFile(file: InputStream, simpleHeaders: Boolean = true): Seq[ParsedRow] = {
    val sheets = parseXSSFExcelFileWithSheetMetadata(file, simpleHeaders)
    sheets.flatMap(_.rows)
  }

  override def parseXSSFExcelFileWithSheetMetadata(file: InputStream, simpleHeaders: Boolean = true): Seq[ParsedSheet] = {
    val pkg = OPCPackage.open(file)
    val sst = new ReadOnlySharedStringsTable(pkg)
    val reader = new XSSFReader(pkg)
    val styles = reader.getStylesTable

    val data = reader.getSheetsData.asInstanceOf[XSSFReader.SheetIterator]

    // can't asScala the SheetIterator as is lubs back to a Seq[InputStream] losing the sheet metadata
    val sheets: mutable.Buffer[ParsedSheet] = mutable.Buffer()
    while (data.hasNext) {
      val sheet = data.next
      val sheetName = data.getSheetName
      val handler = new XslxParser(styles, sst, simpleHeaders)
      val parser = handler.fetchSheetParser
      val sheetSource = new InputSource(sheet)
      parser.parse(sheetSource)
      sheet.close()
      sheets.append(ParsedSheet(sheetName, handler.rows))
    }
    sheets
  }

  class CommentHelper(sheet: SXSSFSheet) {
    private[this] lazy val workbook: SXSSFWorkbook = sheet.getWorkbook
    private[this] lazy val factory: CreationHelper = workbook.getCreationHelper
    private[this] lazy val drawing: SXSSFDrawing = sheet.createDrawingPatriarch()

    def createComment(cell: Cell, text: String): Comment = {
      val anchor = factory.createClientAnchor()

      // By default the comment drawing starts at the end of the cell, is 4 cells wide and 4 rows high
      anchor.setCol1(cell.getColumnIndex)
      anchor.setCol2(cell.getColumnIndex + 4)
      anchor.setRow1(cell.getRowIndex)
      anchor.setRow2(cell.getRowIndex + 4)

      val comment = drawing.createCellComment(anchor)
      comment.setString(factory.createRichTextString(text))
      comment
    }
  }
}

case class ParsedSheet(
  name: String,
  rows: Seq[ParsedRow]
)

case class ParsedRow(
  rowNumber: Int,
  data: Map[String, String]
)

class XslxParser(val styles: StylesTable, val sst: ReadOnlySharedStringsTable, val simpleHeaders: Boolean = true)
  extends SheetContentsHandler with Logging {

  var isParsingHeader = true // flag to parse the first row for column headers
  var columnMap: mutable.Map[Short, String] = scala.collection.mutable.Map[Short, String]()
  val xssfHandler = new XSSFSheetXMLHandler(styles, sst, this, false)

  var rows: scala.collection.mutable.MutableList[ParsedRow] = scala.collection.mutable.MutableList()
  var currentRow: mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()

  def fetchSheetParser: XMLReader = {
    val parser = XmlUtils.getXmlReader
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
      rows += ParsedRow(row, currentRow.toMap)
  }
}

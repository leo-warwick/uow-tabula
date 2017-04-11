package uk.ac.warwick.tabula.services.coursework.docconversion

import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFSheetXMLHandler}
import org.apache.poi.xssf.model.StylesTable
import org.xml.sax.XMLReader
import org.xml.sax.helpers.XMLReaderFactory
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.mutable

abstract class AbstractXslxSheetHandler[A](var styles: StylesTable, var sst: ReadOnlySharedStringsTable, var items: JList[A])
	extends SheetContentsHandler with Logging {

	var lastContents: String = null
	var cellIsString = false
	var isFirstRow = true // flag to skip the first row as it will contain column headers
	var columnMap: mutable.Map[Short, String] = scala.collection.mutable.Map[Short, String]()
	var columnIndex: Int = _
	var currentItem: A = _

	val xssfHandler = new XSSFSheetXMLHandler(styles, sst, this, false)

	def fetchSheetParser: XMLReader = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		parser.setContentHandler(xssfHandler)
		parser
	}

	// don't care about handling this, but required for interface
	override def headerFooter(text: String, isHeader: Boolean, tagName: String){}

	def newCurrentItem: A

	override def startRow(row: Int){
		logger.debug("startRow: " + row.toString)
		if (row > 0) {
			isFirstRow = false
			currentItem = newCurrentItem
		}
	}

	override def endRow(row: Int) {
		if (!isFirstRow)
			items.add(currentItem)
	}
}
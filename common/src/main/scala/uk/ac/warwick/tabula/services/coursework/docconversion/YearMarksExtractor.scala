package uk.ac.warwick.tabula.services.coursework.docconversion

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.JavaImports._
import java.io.InputStream

class YearMarkItem {

	var studentId: String = _
	var mark: String = _
	var academicYear: String = _

	def this(studentId: String, mark: String) = {
		this()
		this.studentId = studentId
		this.mark = mark
	}

	def this(studentId: String, mark: String, academicYear: String) = {
		this()
		this.studentId = studentId
		this.mark = mark
		this.academicYear = academicYear
	}
}

trait YearMarksExtractor {
	/**
		* Method for reading in a xlsx spreadsheet and converting it into a list of YearMarkItems
		*/
	def readXSSFExcelFile(file: InputStream): JList[YearMarkItem]
}

@Service("yearMarksExtractor")
class YearMarksExtractorImpl extends YearMarksExtractor {

	def readXSSFExcelFile(file: InputStream): JList[YearMarkItem] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val markItems: JList[YearMarkItem] = JArrayList()
		val sheetHandler = new YearMarkItemXslxSheetHandler(styles, sst, markItems)
		val parser = sheetHandler.fetchSheetParser
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		markItems.filterNot(markItem => markItem.studentId == null || markItem.mark == null)
	}
}

trait YearMarksExtractorComponent {
	val yearMarksExtractor: YearMarksExtractor
}

trait AutowiringYearMarksExtractorComponent extends YearMarksExtractorComponent {
	val yearMarksExtractor: YearMarksExtractor = Wire[YearMarksExtractor]
}

package uk.ac.warwick.tabula.services.coursework.docconversion

import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.XSSFComment
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConversions._

class YearMarkItemXslxSheetHandler(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[YearMarkItem])
	extends AbstractXslxSheetHandler(styles, sst, markItems) with SheetContentsHandler with Logging {

	override def newCurrentItem = new YearMarkItem()

	override def cell(cellReference: String, formattedValue: String, comment: XSSFComment){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow){
			columnMap(col) = formattedValue
		} else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "Student ID" =>
					currentItem.studentId = formattedValue
				case "Mark" =>
					if(formattedValue.hasText)
						currentItem.mark = formattedValue
				case "Academic year" =>
					if(formattedValue.hasText)
						currentItem.academicYear = formattedValue
				case _ => // ignore anything else
			}
		}
	}
	
}
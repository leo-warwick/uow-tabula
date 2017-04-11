package uk.ac.warwick.tabula.services.coursework.docconversion

import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import org.apache.poi.xssf.usermodel.XSSFComment
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{UserLookupComponent, UserLookupService}

import scala.collection.JavaConversions._

object MarkItemXslxSheetHandler {
	def apply(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[MarkItem], userLookupService: UserLookupService) =
		new MarkItemXslxSheetHandler(styles, sst, markItems) {
			def userLookup = userLookupService
		}
}


abstract class MarkItemXslxSheetHandler(styles: StylesTable, sst: ReadOnlySharedStringsTable, markItems: JList[MarkItem])
	extends AbstractXslxSheetHandler(styles, sst, markItems) with SheetContentsHandler with Logging with UserLookupComponent {

	override def newCurrentItem = new MarkItem()

	override def cell(cellReference: String, formattedValue: String, comment: XSSFComment){
		val col = new CellReference(cellReference).getCol
		if (isFirstRow){
			columnMap(col) = formattedValue
		} else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "University ID" | "ID" =>
					currentItem.universityId = formattedValue
					currentItem.user = userLookup.getUserByWarwickUniId(formattedValue)
				case "Mark" =>
					if(formattedValue.hasText)
						currentItem.actualMark = formattedValue
				case "Grade" =>
					currentItem.actualGrade = formattedValue
				case _ => // ignore anything else
			}
		}
	}

}
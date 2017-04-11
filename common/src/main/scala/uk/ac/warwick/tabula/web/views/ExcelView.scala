package uk.ac.warwick.tabula.web.views
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.JavaImports._

class ExcelView(var filename: String, var workbook: XSSFWorkbook) extends View {

	override def getContentType() = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType)
		response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
		val out = response.getOutputStream
		workbook.write(out)
	}
}

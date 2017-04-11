package uk.ac.warwick.tabula.web.views

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.JavaImports._

class WordView(var filename: String, var document: XWPFDocument) extends View {

	override def getContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(getContentType)
		response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"")
		val out = response.getOutputStream
		document.write(out)
	}
}

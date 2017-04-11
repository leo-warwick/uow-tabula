package uk.ac.warwick.tabula.web.views

import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.JavaImports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.tabula.pdf.PDFGeneratorComponent
import scala.collection.JavaConverters._

class PDFView(filename: String, templateName: String, context: Map[String,_]) extends View {
	this: PDFGeneratorComponent =>

	override def getContentType() = "application/pdf"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) {
		response.setContentType(s"$getContentType; charset=UTF-8")
		response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");

		val mergedModel = JMap((context.toSeq ++ model.asScala.toSeq) :_*)
		val out = response.getOutputStream

		pdfGenerator.renderTemplate(templateName, mergedModel, out)
	}
}

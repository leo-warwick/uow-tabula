package uk.ac.warwick.tabula.web.views

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

class CSVView(var filename: String = "tabula-data.csv", var csv: Any) extends View {
	override def getContentType() = "text/csv"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		response.setContentType(s"$getContentType; charset=UTF-8")
		response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"")
		val out = response.getWriter
		out.println(csv.toString)
	}

	// for testing
	def getAsString: String = csv.toString
}

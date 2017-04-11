package uk.ac.warwick.tabula.web.views

import org.springframework.web.servlet.View
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.JavaImports._
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.data.CalendarOutputter
import uk.ac.warwick.tabula.helpers.StringUtils._

class IcalView(var ical: Calendar) extends View {

	override def getContentType = "text/calendar"

	override def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse): Unit = {
		/*
		 * There's no consistent standard for encoding in the optional
		 * "filename" attribute of Content-Disposition, so you should stick
		 * to the only reliable method of specifying the filename which
		 * is to put it as the last part of the URL path.
		 */
		val dispositionHeader = Option(model.get("filename")) match {
			case Some(fileName: String) if fileName.hasText => "attachment;filename=\"" + fileName.trim + "\""
			case _ => "attachment"
		}

		response.setContentType(s"$getContentType; charset=UTF-8")
		response.setHeader("Content-Disposition", dispositionHeader)
		new CalendarOutputter().output(ical, response.getWriter)
	}
}

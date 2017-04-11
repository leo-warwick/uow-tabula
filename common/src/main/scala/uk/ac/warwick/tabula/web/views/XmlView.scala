package uk.ac.warwick.tabula.web.views

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.web.servlet.View
import uk.ac.warwick.tabula.JavaImports.JMap

import scala.xml.{Elem, MinimizeMode, Utility}

/** Renders an XML element as a document. Adds XML decl and sets text/xml content type. */
class XmlView(xml: Elem, filename: Option[String] = None) extends View {
	def getContentType: String = "text/xml"
	def render(model: JMap[String, _], request: HttpServletRequest, response: HttpServletResponse) {
		filename.foreach(fn => response.setHeader("Content-Disposition", "attachment;filename=\"" + fn + "\""))
		// Scala's main XML library doesn't support XML declarations for some reason. So write one here.
		val writer = response.getWriter
		writer write """<?xml version="1.0" encoding="UTF-8" ?>"""
		writer write "\n"
		writer write Utility.serialize(xml,
			preserveWhitespace = false,
			minimizeTags = MinimizeMode.Always).toString
	}
}

package uk.ac.warwick.tabula.data.model.forms

import scala.xml.NodeSeq

trait FormattedHtml {
	/**
	 * Return a formatted version of the text that can be inserted
	 * WITHOUT escaping.
	 */
	def formattedHtml(string: Option[String]): String = string map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map { p => <p>{ p }</p> }
		(NodeSeq fromSeq nodes).toString
	} getOrElse ("")

	def formattedHtml(string: String): String = formattedHtml(Option(string))
}
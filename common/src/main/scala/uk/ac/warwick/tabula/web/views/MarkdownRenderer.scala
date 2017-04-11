package uk.ac.warwick.tabula.web.views

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser

import uk.ac.warwick.tabula.JavaImports._

trait MarkdownRenderer {
	def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
	private val markdownParser = Parser.builder().extensions(JList(AutolinkExtension.create())).build()
	private val markdownRenderer = HtmlRenderer.builder().build()

	override def renderMarkdown(source: String): String =
		markdownRenderer.render(markdownParser.parse(source))
}
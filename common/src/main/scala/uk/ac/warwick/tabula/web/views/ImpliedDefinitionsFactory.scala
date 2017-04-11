package uk.ac.warwick.tabula.web.views

import org.apache.tiles.request.Request
import org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory
import org.apache.tiles.Attribute
import org.apache.tiles.Definition
import scala.collection.JavaConversions.mapAsJavaMap
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._

/**
 * DefinitionsFactory for Tiles, which first tries the default behaviour
 * of checking the XML files. If none is found and the request view is not
 * an absolute path, it generates a tiles definition on the fly, using a base
 * layout and using the view name to generate a Freemarker path to the body
 * template.
 *
 * e.g. "time/view" will use a body template of /WEB-INF/freemarker/time/view.ftl
 */
class ImpliedDefinitionsFactory extends UnresolvingLocaleDefinitionsFactory with Logging {

	final val FreemarkerRoot = "/WEB-INF/freemarker/"
	final val Extension = ".ftl"
	final val LayoutAttribute = "renderLayout"
	final val DefaultLayout = "base"
	final val BodyTileAttribute = "body"

	/*
   * Return a definition if found in XML. Otherwise if it doesn't start with /,
   * generate our own definition based on the base layout
   */
	override def getDefinition(name: String, ctx: Request): Definition = {
		if (debugEnabled) logger.debug("Rendering " + name)
		val name2 = name // TODO figure out why using name directly ends up null
		super.getDefinition(name, ctx) match {
			case definition: Any => definition
			case _ if !name2.startsWith("/") => new Definition(layoutDefinition(ctx)) {
				addAll(Map(
					BodyTileAttribute -> new Attribute(FreemarkerRoot + name2 + Extension)))
			}
			case _ => null // Let it be handled by FreemarkerServlet
		}
	}

	def layoutDefinition(ctx: Request): Definition = {
		val layout = layoutTemplate(ctx)
		if (debugEnabled) logger.debug("Using layout template " + layout)
		super.getDefinition(layout, ctx)
	}

	def layoutTemplate(ctx: Request): String =
		if (ctx.getAvailableScopes.asScala.contains(Request.REQUEST_SCOPE))
			ctx.getContext(Request.REQUEST_SCOPE).get(LayoutAttribute) match {
				case name: String => name.trim
				case _ => DefaultLayout
			}
		else DefaultLayout

}
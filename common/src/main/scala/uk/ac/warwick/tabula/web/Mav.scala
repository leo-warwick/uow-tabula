package uk.ac.warwick.tabula.web

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View

/**
 * Scala-y modelandview object. There is a return value processor
 * that teaches Spring what to do with a Mav returned from a controller
 * method, so you can return these from methods.
 *
 * Mav("my/viewname",
 *   "objName" -> obj,
 *   "user" -> myUser
 *   )
 */
class Mav() {

	def this(viewName: String) = {
		this()
		this.viewName = viewName
	}

	def this(view: View) = {
		this()
		this.view = view
	}

	var view: View = _
	var viewName: String = _

	var map: mutable.Map[String, Any] = mutable.Map[String, Any]()

	// CSS classes to add to <body>. See #bodyClasses
	var classes: List[String] = Nil

	def addObjects(items: (String, Any)*): Mav = {
		map ++= items
		this
	}

	def addObjects(items: Map[String, Any]): Mav = {
		map ++= items
		this
	}

	// Set the CSS body classes to these strings.
	def bodyClasses(c: String*): Mav = {
		classes = c.toList
		this
	}

	/**
	 * Change to a non-default layout template.
	 *
	 * @see #noLayout
	 */
	def layout(name: String): Mav = addObjects("renderLayout" -> name)

	/**
	 * Add a list of breadcrumbs to display in the navigation.
	 */
	def crumbs(pages: BreadCrumb*): Mav = crumbsList(pages.toSeq)

	/**
	 * Add a secondary list of breadcrumbs to display in the navigation.
	 */
	def secondCrumbs(pages: BreadCrumb*): Mav = crumbsList(pages.toSeq, "secondBreadcrumbs")

	def crumbsList(pages: Seq[BreadCrumb], objectName: String = "breadcrumbs"): Mav = addObjects(objectName -> pages)

	/**
	 * Set a custom title
	 */
	def withTitle(title: String): Mav = addObjects("pageTitle" -> title)

	/**
	 * Sets the layout parameter to "none" to
	 * render the template without any surrounding stuff.
	 *
	 * Also sets embedded->true so views can decide whether to
	 * render certain elements.
	 */
	def noLayout(): Mav = {
		addObjects("embedded" -> true)
		layout("none")
	}

	def noNavigation(): Mav = {
		addObjects("embedded" -> true)
		layout("nonav")
	}

	def embedded: Mav = {
		addObjects("embedded" -> true)
		layout("embedded")
	}

	/**
	 * CustomFreemarkerServlet will use this value if present to set the content type of the response.
	 */
	def contentType(contentType: String): Mav = {
		addObjects("contentType" -> contentType)
		this
	}

	/** Removes layout and sets contentType to text/xml */
	def xml(): Mav = {
		noLayout()
		contentType("text/xml")
	}

	/**
	 * Changes to noLayout if the given thing is true.
	 */
	def noLayoutIf(bool: Boolean): Mav =
		if (bool) noLayout()
		else this

	def toModel: mutable.Map[String, Any] = {
		map ++ bodyClassesItem
	}

	private def bodyClassesItem = classes match {
		case Nil => Nil
		case _ => List("bodyClasses" -> classes.mkString(" "))
	}

	def toModelAndView: ModelAndView = {
		val mav = if (view != null) new ModelAndView(view) else new ModelAndView(viewName)
		mav.addAllObjects(toModel)
		mav
	}

	override def toString: String = {
		val v = if (view != null) view else viewName
		"Mav(" + v + ", " + map + ")"
	}

	override def equals(other: Any): Boolean = other match {
		case m: Mav =>
			view == m.view &&
			viewName == m.viewName &&
			map == m.map
		case _ => false
	}
}

/**
 * ModelAndView builder. Takes a viewname and then any number of Pair objects (which awesomely can be
 * created with the syntax a->b).
 *
 */
object Mav {
	/**
	 * apply() is called when you call the object like Mav(..)
	 *
	 * objects is a repeated list of Pairs. The Map()
	 * constructor accepts the same kind of pair list, so "objects:_*" is used to pass the argument
	 * list through as separate arguments rather than as a single list argument.
	 */
	def apply(view: String, objects: (String, _)*): Mav = new Mav(view).addObjects(objects: _*)
	def apply(view: String, objects: Map[String, _]): Mav = new Mav(view).addObjects(objects)

	def apply(view: View, objects: (String, _)*): Mav = new Mav(view).addObjects(objects: _*)
	def apply(view: View, objects: Map[String, _]): Mav = new Mav(view).addObjects(objects)

	def empty() = new Mav()
}
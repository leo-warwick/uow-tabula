package uk.ac.warwick.tabula.web

/**
 * An object that can be rendered as part of the breadcrumb navigation on the page.
 */
trait BreadCrumb {
	val title: String
	val url: Option[String]
	def linked: Boolean = url.isDefined
	val tooltip: String = ""
	val active: Boolean = false // ID7 navigation active
}

object BreadCrumb {
	/**
	 * Expose Breadcrumb() method for standard breadcrumbs.
	 */
	def apply(title: String, url: String, tooltip: String = null) = Breadcrumbs.Standard(title, Some(url), tooltip)
}

object Breadcrumbs {
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb {
		def setActive(thisActive: Boolean): BreadCrumb = {
			if (!this.active && thisActive) {
				Active(this.title, this.url, this.tooltip)
			} else {
				this
			}
		}
	}
	case class Active(override val title: String, override val url: Option[String], override val tooltip: String) extends BreadCrumb {
		override val active = true
		def setActive(thisActive: Boolean): BreadCrumb = {
			if (this.active && !thisActive) {
				Standard(this.title, this.url, this.tooltip)
			} else {
				this
			}
		}
	}
}
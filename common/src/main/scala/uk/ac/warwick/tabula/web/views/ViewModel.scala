package uk.ac.warwick.tabula.web.views

/**
 * Objects to pass to views for rendering.
 *
 * This is in a bit of a prototype stage - not yet sure if building each menu
 * item in code is going to work.
 */
object ViewModel {

	case class Menu(name: String, icon: String, items: Seq[MenuEntry])

	abstract class MenuEntry(val kind: String)

	case class MenuSeparator() extends MenuEntry("separator")

	case class MenuAction(
			name: String,
			icon: String,
			url: String,
			enabled: Boolean = true,
			disabledPopupText: Option[String] = None) extends MenuEntry("action") {
		def disabledIf(cond: => Boolean, disabledMessage: String): MenuAction = copy(enabled=(!cond), disabledPopupText=Some(disabledMessage))
	}

}

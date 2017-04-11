package uk.ac.warwick.tabula.web.controllers.home

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.home.DismissNotificationCommand
import uk.ac.warwick.tabula.data.model.{Activity, Notification, ToEntityReference}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.MarkdownRendererImpl

@Controller
class DismissNotificationController extends BaseController with ActivityJsonMav with MarkdownRendererImpl {

	@ModelAttribute("dismissCommand")
	def dismissCommand(user: CurrentUser, @PathVariable notification: Notification[_ >: Null <: ToEntityReference ,_]) =
		DismissNotificationCommand(Seq(notification), dismiss=true, user.apparentUser)

	@ModelAttribute("restoreCommand")
	def restoreCommand(user: CurrentUser, @PathVariable notification: Notification[_ >: Null <: ToEntityReference ,_]) =
		DismissNotificationCommand(Seq(notification), dismiss=false, user.apparentUser)


	@RequestMapping(value=Array("/activity/dismiss/{notification}"))
	def dismiss(@ModelAttribute("dismissCommand") command: Appliable[Seq[Activity[_]]]): Mav = {
		val activities = command.apply()
		toMav(activities)
	}

	@RequestMapping(value=Array("/activity/restore/{notification}"))
	def restore(@ModelAttribute("restoreCommand") command: Appliable[Seq[Activity[_]]]): Mav = {
		val activities = command.apply()
		toMav(activities)
	}
}

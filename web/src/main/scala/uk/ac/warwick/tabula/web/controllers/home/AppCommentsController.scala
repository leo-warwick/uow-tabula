package uk.ac.warwick.tabula.web.controllers.home


import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.home.AppCommentCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent

import scala.concurrent.Future

@Controller
@RequestMapping(Array("/comments"))
class AppCommentsController extends BaseController with AutowiringModuleAndDepartmentServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command = AppCommentCommand(user)

	@ModelAttribute("Recipients")
	def Recipients = AppCommentCommand.Recipients

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Future[JBoolean]]): Mav = {
		val hasDeptAdmin = Option(user).exists(_.loggedIn) &&
			moduleAndDepartmentService.getDepartmentByCode(user.apparentUser.getDepartmentCode).flatMap(_.owners.users.headOption).isDefined
		Mav("home/comments",
			"hasDeptAdmin" -> hasDeptAdmin
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Future[JBoolean]], errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("home/comments-success")
		}

	}

}

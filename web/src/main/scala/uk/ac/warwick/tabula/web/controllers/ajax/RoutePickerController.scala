package uk.ac.warwick.tabula.web.controllers.ajax

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, Command, CommandInternal, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/ajax/routepicker/query"))
class RoutePickerController extends BaseController {

	@ModelAttribute("command")
	def command = RoutePickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command")cmd: Appliable[Seq[Route]]): Mav = {
		val routes = cmd.apply()
		Mav(
			new JSONView(
				routes.map(route => Map(
					"id" -> route.id,
					"code" -> route.code,
					"name" -> route.name,
					"department" -> route.adminDepartment.name
				))
			)
		)
	}

}

class RoutePickerCommand extends CommandInternal[Seq[Route]] {

	self: CourseAndRouteServiceComponent =>

	var query: String = _

	def applyInternal(): Seq[Route] = {
		if (query.isEmpty) {
			Seq()
		} else {
			courseAndRouteService.findRoutesNamedLike(query)
		}
	}

}

object RoutePickerCommand {
	def apply() =
		new RoutePickerCommand with Command[Seq[Route]]
		with AutowiringCourseAndRouteServiceComponent
		with ReadOnly with Unaudited with Public
}

package uk.ac.warwick.tabula.web.controllers.ajax

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, Command, CommandInternal, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringSmallGroupServiceComponent, ModuleAndDepartmentServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

case class ModulePickerResult(module: Module, hasSmallGroups: Boolean, hasAssignments: Boolean)

@Controller
@RequestMapping(value = Array("/ajax/modulepicker/query"))
class ModulePickerController extends BaseController {

	@ModelAttribute("command")
	def command = ModulePickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command") cmd: Appliable[Seq[ModulePickerResult]]): Mav = {
		val results = cmd.apply()
		Mav(
			new JSONView(
				results.map(result => Map(
					"id" -> result.module.id,
					"code" -> result.module.code,
					"name" -> result.module.name,
					"department" -> result.module.adminDepartment.name,
					"hasSmallGroups" -> result.hasSmallGroups,
					"hasAssignments" -> result.hasAssignments
				))
			)
		)
	}

}

class ModulePickerCommand extends CommandInternal[Seq[ModulePickerResult]] {

	self: ModuleAndDepartmentServiceComponent with SmallGroupServiceComponent =>

	var query: String = _
	var checkAssignments: Boolean = _
	var checkGroups: Boolean = _

	def applyInternal(): Seq[ModulePickerResult] = {
		if (!query.hasText) {
			Seq()
		} else {
			val modules: Seq[Module] = moduleAndDepartmentService.findModulesNamedLike(query)
			modules.map {
				case (module) =>
					ModulePickerResult(module,
						if (checkGroups) smallGroupService.hasSmallGroups(module) else false,
						if (checkAssignments) moduleAndDepartmentService.hasAssignments(module) else false)
			}
		}
	}

}

object ModulePickerCommand {
	def apply() = new ModulePickerCommand with Command[Seq[ModulePickerResult]] with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringSmallGroupServiceComponent
	with ReadOnly with Unaudited with Public
}

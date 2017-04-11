package uk.ac.warwick.tabula.web.controllers.groups.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.admin.AllocateStudentsToGroupsTemplateCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value=Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/allocate/template"))
class AllocateStudentsTemplateController extends BaseController {

	type AllocateStudentsToGroupsTemplateCommand = Appliable[ExcelView]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): AllocateStudentsToGroupsTemplateCommand =
		AllocateStudentsToGroupsTemplateCommand(module, set)

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: AllocateStudentsToGroupsTemplateCommand): ExcelView = {
		cmd.apply()
	}

}

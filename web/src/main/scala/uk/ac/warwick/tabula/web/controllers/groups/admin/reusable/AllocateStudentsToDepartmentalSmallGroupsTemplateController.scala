package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.reusable.AllocateStudentsToDepartmentalSmallGroupsTemplateCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/{academicYear}/groups/reusable/{smallGroupSet}/template"))
class AllocateStudentsToDepartmentalSmallGroupsTemplateController extends BaseController {

	type AllocateStudentsToDepartmentalSmallGroupsTemplateCommand = Appliable[ExcelView]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable("smallGroupSet") set: DepartmentSmallGroupSet): AllocateStudentsToDepartmentalSmallGroupsTemplateCommand =
		AllocateStudentsToDepartmentalSmallGroupsTemplateCommand(department, set)

	@RequestMapping
	def getTemplate(@Valid @ModelAttribute("command") cmd: AllocateStudentsToDepartmentalSmallGroupsTemplateCommand): ExcelView = {
		cmd.apply()
	}

}

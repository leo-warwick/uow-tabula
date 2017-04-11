package uk.ac.warwick.tabula.web.controllers.admin.department
import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.admin.department.AddSubDepartmentCommand
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.FilterRule

@Controller
@RequestMapping(value = Array("/admin/department/{department}/subdepartment/new"))
class AddSubDepartmentController extends AdminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("allFilterRules")
	def allFilterRules: Seq[FilterRule] = Department.FilterRule.allFilterRules

	@ModelAttribute("addSubDepartmentCommand")
	def command(@PathVariable department: Department) = AddSubDepartmentCommand(mandatory(department))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm() = Mav("admin/department/add/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("addSubDepartmentCommand") command: Appliable[Department], errors: Errors): Mav = {
		if (errors.hasErrors) showForm()
		else {
			val subDepartment = command.apply()
			Redirect(Routes.admin.department(subDepartment))
		}
	}

}

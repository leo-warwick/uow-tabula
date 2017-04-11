package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.admin.department.{EditDepartmentCommand, EditDepartmentCommandState}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Department.FilterRule

@Controller
@RequestMapping(value = Array("/admin/department/{department}/edit"))
class EditDepartmentController extends AdminController {

	validatesSelf[SelfValidating]
	type EditDepartmentCommand = Appliable[Department] with EditDepartmentCommandState

	@ModelAttribute("allFilterRules")
	def allFilterRules: Seq[FilterRule] = Department.FilterRule.allFilterRules

	@ModelAttribute("editDepartmentCommand")
	def command(@PathVariable department: Department): EditDepartmentCommand = EditDepartmentCommand(mandatory(department))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm() = Mav("admin/department/edit/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("editDepartmentCommand") command: EditDepartmentCommand, errors: Errors): Mav = {
		if (errors.hasErrors) showForm()
		else {
			val department = command.apply()
			Redirect(Routes.admin.department(department))
		}
	}

}

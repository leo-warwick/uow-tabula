package uk.ac.warwick.tabula.web.controllers.admin.modules
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid

import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.{Department, Module}
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.commands.admin.modules.AddModuleCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}

@Controller
@RequestMapping(value = Array("/admin/department/{dept}/module/new"))
class AddModuleController extends AdminController {

	// set up self validation for when @Valid is used
	type AddModuleCommand = Appliable[Module]
	validatesSelf[SelfValidating]

	@ModelAttribute("addModuleCommand")
	def command(@PathVariable("dept") department: Department): AddModuleCommand = AddModuleCommand(mandatory(department))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@PathVariable("dept") department: Department): Mav = {
		Mav("admin/modules/add/form",
			"department" -> department
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("addModuleCommand") cmd: AddModuleCommand, errors: Errors, @PathVariable("dept") department: Department): Mav = {
		if (errors.hasErrors) {
			showForm(department)
		} else {
			val module = cmd.apply()
			Redirect(Routes.admin.module(module))
		}
	}

}

package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}

/**
	* Controller that handles the multi-step process of creating many assignments from SITS data.
	*
	* - selectionForm() is called which displays items with checkboxes to choose which assignments
	* to import.
	* - submit to optionsForm() which is a similar form where we select assignments and set options on them
	* - AssignmentSharedOptionsController handles the options screen, and when that passes validation we
	* copy all the form fields into the main page using javascript.
	* - To set dates we open a static popup which then copies the values into place with javascript
	* - Before submitting we make an AJAX call to ajaxValidation() to display any errors.
	* - Finally we submit everything to submit().
	*/

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/department/{department}/setup-assignments"))
class AddSitsAssignmentsController extends CourseworkController with DepartmentScopedController
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AcademicYearScopedController {

	override def departmentPermission: Permission = Permissions.Assignment.ImportFromExternalSystem

	type AddSitsAssignmentsCommand = Appliable[Seq[Assignment]]
		with PopulatesAddSitsAssignmentsCommand  with  AddSitsAssignmentsCommandOnBind with AddSitsAssignmentsValidation


	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		AddSitsAssignmentsCommand(mandatory(department), mandatory(user))

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))


	val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

	//Command sets it to default -AcademicYear.guessSITSAcademicYearByDate(DateTime.now.plusMonths(3))
	@ModelAttribute("academicYearChoices")
	def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(0, 1).asJava

	// The initial load of page 1, where we select the items to import.
	@RequestMapping(method = Array(GET))
	def selectionForm(
		@ModelAttribute("command") cmd: AddSitsAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		cmd.populate()
		getMav(department).addObjects("action" -> "select")
	}

	// The shared Mav for most of the request mappings
	def getMav(department: Department) = {
		Mav(s"$urlPrefix/admin/assignments/batch_new_sits_select")
	}
	// Change the academic year; restarts from scratch
	@RequestMapping(method = Array(POST), params = Array("action=change-year"))
	def changeYear(
		@ModelAttribute("command") cmd: AddSitsAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav =
	selectionForm(cmd, errors, department)

	// Reloads page 1 with a POST, to show any updated information if necessary.
	@RequestMapping(method = Array(POST), params = Array("action=refresh-select"))
	def refreshSelectionForm(
		@ModelAttribute("command") cmd: AddSitsAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		getMav(department).addObjects("action" -> "select")
	}

	// Loads page 2 where we set options on all the assignments.
	@RequestMapping(method = Array(POST), params = Array("action=options"))
	def optionsForm(
		@ModelAttribute("command") cmd: AddSitsAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		cmd.validateNames(errors)
		getMav(department).addObjects("action" -> "options")
	}

	// Do validation and return as a chunk of HTML errors.
	@RequestMapping(method = Array(POST), params = Array("action=validate"))
	def ajaxValidation(@Valid @ModelAttribute("command") cmd: AddSitsAssignmentsCommand, errors: Errors): Mav = {
		Mav(s"$urlPrefix/admin/assignments/batch_new_validation").noLayout()
	}

	// Final step where we actually do the work.
	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(
		@Valid @ModelAttribute("command") cmd: AddSitsAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		if (errors.hasErrors) {
			getMav(department).addObjects("action" -> "options")
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(department, academicYear))
		}
	}
}
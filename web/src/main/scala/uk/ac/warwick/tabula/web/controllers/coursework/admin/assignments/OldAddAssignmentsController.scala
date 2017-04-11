package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.assignments.{AddAssignmentsCommand, AddAssignmentsCommandOnBind, AddAssignmentsValidation, PopulatesAddAssignmentsCommand}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConversions._

/**
 * Controller that handles the multi-step process of creating many assignments from SITS data.
 *
 * - selectionForm() is called which displays items with checkboxes to choose which assignments
 *      to import.
 * - submit to optionsForm() which is a similar form where we select assignments and set options on them
 * - AssignmentSharedOptionsController handles the options screen, and when that passes validation we
 *   copy all the form fields into the main page using javascript.
 * - To set dates we open a static popup which then copies the values into place with javascript
 * - Before submitting we make an AJAX call to ajaxValidation() to display any errors.
 * - Finally we submit everything to submit().
 */
@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/department/{department}/setup-assignments"))
class OldAddAssignmentsController extends OldCourseworkController {

	type AddAssignmentsCommand = Appliable[Seq[Assignment]]
		with PopulatesAddAssignmentsCommand with AddAssignmentsCommandOnBind with AddAssignmentsValidation

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		AddAssignmentsCommand(mandatory(department), user)

	@ModelAttribute("academicYearChoices")
	def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(0, 1)

	// The initial load of page 1, where we select the items to import.
	@RequestMapping(method = Array(GET))
	def selectionForm(
		@ModelAttribute("command") cmd: AddAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		cmd.populate()
		getMav(department).addObjects("action" -> "select")
	}

	// Change the academic year; restarts from scratch
	@RequestMapping(method = Array(POST), params = Array("action=change-year"))
	def changeYear(
		@ModelAttribute("command") cmd: AddAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav =
		selectionForm(cmd, errors, department)

	// Reloads page 1 with a POST, to show any updated information if necessary.
	@RequestMapping(method = Array(POST), params = Array("action=refresh-select"))
	def refreshSelectionForm(
		@ModelAttribute("command") cmd: AddAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		getMav(department).addObjects("action" -> "select")
	}

	// Loads page 2 where we set options on all the assignments.
	@RequestMapping(method = Array(POST), params = Array("action=options"))
	def optionsForm(
		@ModelAttribute("command") cmd: AddAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		cmd.validateNames(errors)
		getMav(department).addObjects("action" -> "options")
	}

	// Do validation and return as a chunk of HTML errors.
	@RequestMapping(method = Array(POST), params = Array("action=validate"))
	def ajaxValidation(@Valid @ModelAttribute("command") cmd: AddAssignmentsCommand, errors: Errors): Mav = {
		Mav(s"$urlPrefix/admin/assignments/batch_new_validation").noLayout()
	}

	// Final step where we actually do the work.
	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(
		@Valid @ModelAttribute("command") cmd: AddAssignmentsCommand,
		errors: Errors,
		@PathVariable department: Department
	): Mav = {
		if (errors.hasErrors) {
			optionsForm(cmd, errors, department)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(department))
		}
	}

	// The shared Mav for most of the request mappings
	def getMav(department: Department): Mav = {
		Mav(s"$urlPrefix/admin/assignments/batch_new_select")
			.crumbs(Breadcrumbs.Department(department))
	}

}

package uk.ac.warwick.tabula.web.controllers.coursework.admin.assignments

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.commands.{UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConverters._

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value=Array("/${cm1.prefix}/admin/module/{module}/assignments/new"))
class OldAddAssignmentController extends OldCourseworkController {

	@Autowired var assignmentService: AssessmentService = _

	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava
	}

	validatesSelf[AddAssignmentCommand]

	@ModelAttribute def addAssignmentForm(@PathVariable module: Module) =
		new AddAssignmentCommand(mandatory(module))

	// Used for initial load and for prefilling from a chosen assignment
	@RequestMapping()
	def form(form: AddAssignmentCommand): Mav = {
		form.afterBind()
		form.prefillFromRecentAssignment()
		showForm(form)
	}

	@RequestMapping(method = Array(POST), params = Array("action=submit"))
	def submit(@Valid form: AddAssignmentCommand, errors: Errors): Mav = {
		form.afterBind()
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}
	}

	@RequestMapping(method = Array(POST), params = Array("action=refresh"))
	def submit(form: AddAssignmentCommand): Mav = {
		// No validation here
		form.afterBind()
		showForm(form)
	}

	@RequestMapping(method = Array(POST), params = Array("action=update"))
	def update(@Valid form: AddAssignmentCommand, errors: Errors): Mav = {
		form.afterBind()
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.assignment.edit(form.assignment) + "?open")
		}
	}

	def showForm(form: AddAssignmentCommand): Mav = {
		val module = form.module

		Mav(s"$urlPrefix/admin/assignments/new",
			"department" -> module.adminDepartment,
			"module" -> module,
			"academicYear" -> form.academicYear,
			"availableUpstreamGroups" -> form.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> form.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> form.assessmentGroups,
			"collectSubmissions" -> form.collectSubmissions,
			"maxWordCount" -> Assignment.MaximumWordCount,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}

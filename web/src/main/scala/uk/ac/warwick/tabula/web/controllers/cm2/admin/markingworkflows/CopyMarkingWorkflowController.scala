package uk.ac.warwick.tabula.web.controllers.cm2.admin.markingworkflows

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.cm2.markingworkflows.{CopyMarkingWorkflowCommand, CopyMarkingWorkflowState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.web.{Mav, Routes}


@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear}/markingworkflows/{workflow}/copy"))
class CopyMarkingWorkflowController extends CM2MarkingWorkflowController {

	var messageSource: MessageSource = Wire.auto[MessageSource]

	type CopyMarkingWorkflowCommand = Appliable[CM2MarkingWorkflow] with CopyMarkingWorkflowState

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable workflow: CM2MarkingWorkflow) =
		CopyMarkingWorkflowCommand(mandatory(department), mandatory(workflow))

	@RequestMapping
	def submitForm(
		@PathVariable department: Department,
		@Valid @ModelAttribute("command") cmd: CopyMarkingWorkflowCommand,
		errors: Errors
	): Mav = {
		val model = if(errors.hasErrors) {
			"actionErrors" -> errors.getAllErrors.asScala.map(
				e => messageSource.getMessage(e.getCode, e.getArguments, null)
			).mkString(", ")
		} else {
			"copiedWorkflow" -> cmd.apply().id
		}
		val currentAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		Redirect(Routes.cm2.admin.workflows(department, currentAcademicYear), model)
	}

}

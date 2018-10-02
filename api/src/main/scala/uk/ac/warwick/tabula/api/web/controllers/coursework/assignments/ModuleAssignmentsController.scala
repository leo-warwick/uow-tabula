package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.servlet.http.HttpServletResponse
import org.joda.time.DateTime
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, AssignmentToJsonConverter, AssignmentToXmlConverter}
import uk.ac.warwick.tabula.commands.coursework.assignments.{AddAssignmentCommand, ModifyAssignmentCommand}
import uk.ac.warwick.tabula.commands.{UpstreamGroup, UpstreamGroupPropertyEditor, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.helpers.XmlUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView, XmlErrorView, XmlView}
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

abstract class ModuleAssignmentsController extends ApiController
	with AssignmentToJsonConverter
	with AssignmentToXmlConverter
	with AssessmentMembershipInfoToJsonConverter

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments"))
class ListAssignmentsForModuleController extends ModuleAssignmentsController {

	@ModelAttribute("listCommand")
	def command(@PathVariable module: Module, user: CurrentUser): ViewViewableCommand[Module] =
		new ViewViewableCommand(Permissions.Module.ManageAssignments, mandatory(module))

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("listCommand") command: ViewViewableCommand[Module], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val module = command.apply()
			val assignments = module.assignments.asScala.filter { assignment =>
				!assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
			}

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"academicYear" -> Option(academicYear).map { _.toString }.orNull,
				"assignments" -> assignments.map(jsonAssignmentObject)
			)))
		}
	}

	@RequestMapping(method = Array(GET), produces = Array("application/xml"))
	def listXML(@ModelAttribute("listCommand") command: ViewViewableCommand[Module], errors: Errors, @RequestParam(required = false) academicYear: AcademicYear): Mav = {
		if (errors.hasErrors) {
			Mav(new XmlErrorView(errors))
		} else {
			val module = command.apply()
			val assignments = module.assignments.asScala.filter { assignment =>
				!assignment.deleted && (academicYear == null || academicYear == assignment.academicYear)
			}

			Mav(new XmlView(
				<assignments>
					{ assignments.map(xmlAssignmentObject) }
				</assignments> % Map(
					"success" -> true,
					"status" -> "ok",
					"academicYear" -> Option(academicYear).map { _.toString }.orNull
				)
			))
		}
	}
}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments"))
class CreateAssignmentController extends ModuleAssignmentsController {

	@ModelAttribute("createCommand")
	def command(@PathVariable module: Module): AddAssignmentCommand = {
		val cmd = new AddAssignmentCommand(module)
		cmd.cm2Assignment = true
		cmd
	}

	@InitBinder(Array("createCommand"))
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def create(@RequestBody request: CreateAssignmentRequest, @ModelAttribute("createCommand") command: AddAssignmentCommand, errors: Errors)(implicit response: HttpServletResponse): Mav = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)
		command.afterBind()

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val assignment = command.apply()

			response.setStatus(HttpStatus.CREATED.value())
			response.addHeader("Location", toplevelUrl + Routes.api.assignment(assignment))

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"assignment" -> jsonAssignmentObject(assignment)
			)))
		}
	}
}

trait AssignmentPropertiesRequest[A <: ModifyAssignmentCommand] extends JsonApiRequest[A]
	with BooleanAssignmentProperties {

	@BeanProperty var name: String = null
	@BeanProperty var openDate: DateTime = null
	@BeanProperty var closeDate: DateTime = null
	@BeanProperty var academicYear: AcademicYear = null
	@BeanProperty var feedbackTemplate: FeedbackTemplate = null
	@BeanProperty var markingWorkflow: CM2MarkingWorkflow = null
	@BeanProperty var includeUsers: JList[String] = null
	@BeanProperty var upstreamGroups: JList[UpstreamGroup] = null
	@BeanProperty var fileAttachmentLimit: JInteger = null
	@BeanProperty var fileAttachmentTypes: JList[String] = null
	@BeanProperty var individualFileSizeLimit: JInteger = null
	@BeanProperty var minWordCount: JInteger = null
	@BeanProperty var maxWordCount: JInteger = null
	@BeanProperty var wordCountConventions: String = null

	override def copyTo(state: A, errors: Errors) {
		if (Option(openDate).isEmpty && Option(closeDate).nonEmpty) {
			if (openEnded) openDate = DateTime.now
			else openDate = closeDate.minusWeeks(2)
		}

		Option(name).foreach { state.name = _ }
		Option(openDate).foreach { state.openDate = _ }
		Option(closeDate).foreach { state.closeDate = _ }
		Option(academicYear).foreach { state.academicYear = _ }
		Option(feedbackTemplate).foreach { state.feedbackTemplate = _ }
		Option(markingWorkflow).foreach { state.markingWorkflow = _ }
		Option(includeUsers).foreach { list => state.massAddUsers = list.asScala.mkString("\n") }
		Option(upstreamGroups).foreach { state.upstreamGroups = _ }
		Option(fileAttachmentLimit).foreach { state.fileAttachmentLimit = _ }
		Option(fileAttachmentTypes).foreach { state.fileAttachmentTypes = _ }
		Option(individualFileSizeLimit).foreach { state.individualFileSizeLimit = _ }
		Option(minWordCount).foreach { state.wordCountMin = _ }
		Option(maxWordCount).foreach { state.wordCountMax = _ }
		Option(wordCountConventions).foreach { state.wordCountConventions = _ }
		Option(openEnded).foreach { state.openEnded = _ }
		Option(collectMarks).foreach { state.collectMarks = _ }
		Option(collectSubmissions).foreach { state.collectSubmissions = _ }
		Option(restrictSubmissions).foreach { state.restrictSubmissions = _ }
		Option(allowLateSubmissions).foreach { state.allowLateSubmissions = _ }
		Option(allowResubmission).foreach { state.allowResubmission = _ }
		Option(displayPlagiarismNotice).foreach { state.displayPlagiarismNotice = _ }
		Option(allowExtensions).foreach { state.allowExtensions = _ }
		Option(extensionAttachmentMandatory).foreach { state.extensionAttachmentMandatory = _ }
		Option(allowExtensionsAfterCloseDate).foreach { state.allowExtensionsAfterCloseDate = _ }
		Option(summative).foreach { state.summative = _ }
		Option(dissertation).foreach { state.dissertation = _ }
		Option(publishFeedback).foreach { state.publishFeedback = _ }
		Option(includeInFeedbackReportWithoutSubmissions).foreach { state.includeInFeedbackReportWithoutSubmissions = _ }
		Option(automaticallyReleaseToMarkers).foreach { state.automaticallyReleaseToMarkers = _ }
		Option(automaticallySubmitToTurnitin).foreach { state.automaticallySubmitToTurnitin = _ }
		Option(hiddenFromStudents).foreach { state.hiddenFromStudents = _ }
	}

}

class CreateAssignmentRequest extends AssignmentPropertiesRequest[AddAssignmentCommand] {

	// Default values
	includeUsers = JArrayList()
	upstreamGroups = JArrayList()
	fileAttachmentLimit = 1
	fileAttachmentTypes = JArrayList()
	wordCountConventions = "Exclude any bibliography or appendices."

}
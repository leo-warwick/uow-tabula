package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.helpers._
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.commands.coursework.assignments._
import uk.ac.warwick.tabula.data.model.forms.{FileFormValue, IntegerFormValue}
import uk.ac.warwick.tabula.helpers.coursework.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.JavaImports._

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import AssignmentController._
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, AutowiringZipServiceComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent

object AssignmentController {
	type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest with BindListener with SelfValidating
}

abstract class AssignmentController extends ApiController
	with AssignmentToJsonConverter
	with AssessmentMembershipInfoToJsonConverter
	with AssignmentStudentToJsonConverter
	with ReplacingAssignmentStudentMessageResolver
	with GetAssignmentApiFullOutput {

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	def getAssignmentMav(command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, assignment: Assignment): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			) ++ outputJson(assignment, results)))
		}
	}
}

@Controller
@RequestMapping(
	method = Array(RequestMethod.GET),
	value = Array("/v1/module/{module}/assignments/{assignment}"),
	params = Array("!universityId"),
	produces = Array("application/json"))
class GetAssignmentController extends AssignmentController with GetAssignmentApi with GetAssignmentApiFullOutput {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(
	method = Array(RequestMethod.PUT),
	value = Array("/v1/module/{module}/assignments/{assignment}"),
	params = Array("!universityId"),
	produces = Array("application/json"))
class EditAssignmentController extends AssignmentController with EditAssignmentApi {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(
	method = Array(RequestMethod.DELETE),
	value = Array("/v1/module/{module}/assignments/{assignment}"),
	params = Array("!universityId"),
	produces = Array("application/json"))
class DeleteAssignmentController extends AssignmentController with DeleteAssignmentApi {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(value = Array("/v1/module/{module}/assignments/{assignment}"), params = Array("universityId"))
class AssignmentCreateSubmissionController extends ApiController
	with CreateSubmissionApi
	with SubmissionToJsonConverter {
	validatesSelf[SelfValidating]
}

trait GetAssignmentApi {
	self: AssignmentController with GetAssignmentApiOutput =>

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def getIt(@Valid @ModelAttribute("getCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment): Mav = {
		// Return the GET representation
		getAssignmentMav(command, errors, assignment)

	}

	@InitBinder(Array("getCommand"))
	def getBinding(binder: WebDataBinder): Unit = {
		binder.registerCustomEditor(classOf[CourseworkFilter], new AbstractPropertyEditor[CourseworkFilter] {
			override def fromString(name: String): CourseworkFilter = CourseworkFilters.of(name)
			override def toString(filter: CourseworkFilter): String = filter.getName
		})
	}
}

trait GetAssignmentApiOutput {
	def outputJson(assignment: Assignment, results: SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults): Map[String, Any]
}

trait GetAssignmentApiFullOutput extends GetAssignmentApiOutput {
	self: ApiController with AssignmentToJsonConverter with AssignmentStudentToJsonConverter =>

	def outputJson(assignment: Assignment, results: SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults) = Map(
		"assignment" -> jsonAssignmentObject(assignment),
		"genericFeedback" -> assignment.genericFeedback,
		"students" -> results.students.map(jsonAssignmentStudentObject)
	)
}

trait EditAssignmentApi {
	self: AssignmentController with AssignmentToJsonConverter with AssignmentStudentToJsonConverter =>

	@ModelAttribute("editCommand")
	def editCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): EditAssignmentCommand =
		new EditAssignmentCommand(module, assignment, user)

	@RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def edit(@RequestBody request: EditAssignmentRequest, @ModelAttribute("editCommand") command: EditAssignmentCommand, errors: Errors): Mav = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)
		command.afterBind()

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val assignment = command.apply()

			// Return the GET representation
			getAssignmentMav(getCommand(assignment.module, assignment), errors, assignment)
		}
	}
}

class EditAssignmentRequest extends AssignmentPropertiesRequest[EditAssignmentCommand] {

	// set defaults to null
	openEnded = null
	collectMarks = null
	collectSubmissions = null
	restrictSubmissions = null
	allowLateSubmissions = null
	allowResubmission = null
	displayPlagiarismNotice = null
	allowExtensions = null
	extensionAttachmentMandatory = null
	allowExtensionsAfterCloseDate = null
	summative = null
	dissertation = null
	includeInFeedbackReportWithoutSubmissions = null
	automaticallyReleaseToMarkers = null
	automaticallySubmitToTurnitin = null

}

trait DeleteAssignmentApi {
	self: AssignmentController =>

	@ModelAttribute("deleteCommand")
	def deleteCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): DeleteAssignmentCommand = {
		val command = new DeleteAssignmentCommand(module, assignment)
		command.confirm = true
		command
	}

	@RequestMapping(method = Array(DELETE), produces = Array("application/json"))
	def delete(@Valid @ModelAttribute("deleteCommand") command: DeleteAssignmentCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			)))
		}
	}
}

trait CreateSubmissionApi {
	self: ApiController with SubmissionToJsonConverter =>

	@ModelAttribute("createCommand")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @RequestParam("universityId") member: Member): SubmitAssignmentCommandInternal with ComposableCommand[Submission] with SubmitAssignmentBinding with SubmitAssignmentOnBehalfOfPermissions with SubmitAssignmentDescription with SubmitAssignmentValidation with SubmitAssignmentNotifications with SubmitAssignmentTriggers with AutowiringSubmissionServiceComponent with AutowiringFeaturesComponent with AutowiringZipServiceComponent with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent =
		SubmitAssignmentCommand.onBehalfOf(module, assignment, member)

	// Two ways into this - either uploading files in advance to the attachments API or submitting a multipart request
	@RequestMapping(method = Array(POST), consumes = Array("multipart/mixed"), produces = Array(MediaType.APPLICATION_JSON_VALUE))
	def create(@RequestPart("submission") request: CreateSubmissionRequest, @RequestPart("attachments") files: JList[MultipartFile], @ModelAttribute("createCommand") command: SubmitAssignmentCommand, errors: BindingResult)(implicit response: HttpServletResponse): Mav = {
		request.copyTo(command, errors)

		command.assignment.attachmentField.map { _.id }.foreach { fieldId =>
			command.fields.get(fieldId).asInstanceOf[FileFormValue].file.upload.addAll(files)
		}

		doCreate(command, errors)
	}

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array(MediaType.APPLICATION_JSON_VALUE))
	def create(@RequestBody request: CreateSubmissionRequest, @ModelAttribute("createCommand") command: SubmitAssignmentCommand, errors: BindingResult)(implicit response: HttpServletResponse): Mav = {
		request.copyTo(command, errors)

		doCreate(command, errors)
	}

	private def doCreate(command: SubmitAssignmentCommand, errors: BindingResult)(implicit response: HttpServletResponse) = {
		command.onBind(errors)

		globalValidator.validate(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val submission = command.apply()

			response.setStatus(HttpStatus.CREATED.value())
			response.addHeader("Location", toplevelUrl + Routes.api.submission(submission))

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"submission" -> jsonSubmissionObject(submission)
			)))
		}
	}

}

@JsonAutoDetect
class CreateSubmissionRequest extends JsonApiRequest[SubmitAssignmentRequest] {

	@BeanProperty var attachments: JList[FileAttachment] = JArrayList()
	@BeanProperty var wordCount: JInteger = null
	@BeanProperty var useDisability: JBoolean = null
	@BeanProperty var plagiarismDeclaration: JBoolean = false

	override def copyTo(state: SubmitAssignmentRequest, errors: Errors): Unit = {
		attachments.asScala.foreach { attachment =>
			state.assignment.attachmentField.map { _.id }.foreach { fieldId =>
				state.fields.get(fieldId).asInstanceOf[FileFormValue].file.attached.add(attachment)
			}
		}

		Option(wordCount).foreach { value =>
			state.assignment.wordCountField.map { _.id }.foreach { fieldId =>
				state.fields.get(fieldId).asInstanceOf[IntegerFormValue].value = value
			}
		}

		state.useDisability = useDisability
		state.plagiarismDeclaration = plagiarismDeclaration
	}

}
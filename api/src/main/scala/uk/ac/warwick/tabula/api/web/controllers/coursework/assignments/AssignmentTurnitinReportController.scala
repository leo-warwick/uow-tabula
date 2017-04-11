package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.coursework.turnitin.{TurnitinReportErrorWithMessage, ViewPlagiarismReportCommand, ViewPlagiarismReportRequest}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONRequestFailedView, JSONView}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}/turnitin-report/{attachment}"))
class AssignmentTurnitinReportController extends ApiController
	with GenerateTurnitinReportUriApi {
	validatesSelf[SelfValidating]
}

trait GenerateTurnitinReportUriApi {
	self: ApiController =>

	type ViewPlagiarismReportCommand = ViewPlagiarismReportCommand.CommandType

	@ModelAttribute("generateReportUriCommand")
	def generateReportUriCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable attachment: FileAttachment): ViewPlagiarismReportCommand =
		ViewPlagiarismReportCommand(module, assignment, attachment)

	@RequestMapping(method = Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array(MediaType.APPLICATION_JSON_VALUE))
	def create(@RequestBody request: GenerateTurnitinReportRequest, @ModelAttribute("generateReportUriCommand") command: ViewPlagiarismReportCommand, errors: BindingResult): Mav = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Left(uri) =>
				Mav(new JSONView(Map(
					"success" -> true,
					"status" -> "ok",
					"reportUrl" -> uri.toString
				)))

			case Right(error: TurnitinReportErrorWithMessage) =>
				Mav(new JSONRequestFailedView(Seq(Map(
					"code" -> error.code,
					"message" -> error.message
				))))

			case Right(error) =>
				Mav(new JSONRequestFailedView(Seq(Map(
					"code" -> error.code
				))))
		}
	}

}

@JsonAutoDetect
class GenerateTurnitinReportRequest extends JsonApiRequest[ViewPlagiarismReportRequest] {

	@BeanProperty var viewer: User = _

	override def copyTo(state: ViewPlagiarismReportRequest, errors: Errors): Unit = {
		state.viewer = viewer
	}

}
package uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.coursework.turnitinlti.{TurnitinLtiConformanceOutcomesServiceCommand, TurnitinLtiConformanceOutcomesServiceCommandResponse, TurnitinLtiConformanceOutcomesServiceCommandState}

import scala.xml.{Elem, XML}
import uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti.TurnitinLtiConformanceOutcomesServiceController.TurnitinLtiConformanceOutcomesServiceCommand

object TurnitinLtiConformanceOutcomesServiceController {
	type TurnitinLtiConformanceOutcomesServiceCommand = Appliable[TurnitinLtiConformanceOutcomesServiceCommandResponse]
	with TurnitinLtiConformanceOutcomesServiceCommandState
}


@Controller
@RequestMapping(Array("/v1/turnitin/turnitin-lti-outcomes/assignment/{assignment}"))
class TurnitinLtiConformanceOutcomesServiceController extends ApiController
	with TurnitinLtiConformanceOutcomesServiceApi

trait TurnitinLtiConformanceOutcomesServiceApi {
	self: ApiController =>

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) =
		TurnitinLtiConformanceOutcomesServiceCommand(assignment)

	// Could configure an xml transformer instead, if we ever needed to do this for real, rather than getting the whole RequestBody and parsing it ourselves.
	@RequestMapping(method=Array(POST))
	def inspectResponse(
		@RequestBody body: String,
		@ModelAttribute("command") command: TurnitinLtiConformanceOutcomesServiceCommand,
		@PathVariable assignment: Assignment,
		errors: Errors
	 ): Elem = {
		command.body = body
		val response = command.apply()
		XML.loadString(response.xmlString)
	}
}




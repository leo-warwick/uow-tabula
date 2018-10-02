package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.submissions

import javax.validation.Valid
import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.submissions.MemberSubmissionsController.ViewMemberSubmissionsCommand
import uk.ac.warwick.tabula.api.web.helpers.{SubmissionInfoToJsonConverter, SubmissionToJsonConverter}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.{ListMemberSubmissionsCommand, ListSubmissionsResult}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringExtensionServiceComponent, AutowiringSubmissionServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

abstract class MemberSubmissionsController extends ApiController
	with SubmissionToJsonConverter
	with SubmissionInfoToJsonConverter
	with AutowiringExtensionServiceComponent
	with AutowiringUserLookupComponent
	with AutowiringSubmissionServiceComponent

object MemberSubmissionsController {
	type ViewMemberSubmissionsCommand = Appliable[ListSubmissionsResult]
}

@Controller
@RequestMapping(
	method = Array(RequestMethod.GET),
	value = Array("/v1/submissions/member/{member}")
)
class GetMemberSubmissionsController extends MemberSubmissionsController
	with GetMemberSubmissionsApi

trait GetMemberSubmissionsApi {
	self: MemberSubmissionsController =>

	validatesSelf[SelfValidating]

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable member: Member,
		@RequestParam(value="fromDate", required=false) fromDate: LocalDate,
		@RequestParam(value="toDate", required=false) toDate: LocalDate): ViewMemberSubmissionsCommand =
		ListMemberSubmissionsCommand(member, fromDate, toDate)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@Valid @ModelAttribute("listCommand") command: ViewMemberSubmissionsCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"fromDate" ->  DateFormats.IsoDateTime.print(results.fromDate),
				"toDate" -> DateFormats.IsoDateTime.print(results.toDate),
				"submissions" -> results.submissions.map(jsonSubmissionInfoObject)
			)))
		}
	}
}
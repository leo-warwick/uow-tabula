package uk.ac.warwick.tabula.api.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, SmallGroupEventToJsonConverter, SmallGroupSetToJsonConverter, SmallGroupToJsonConverter}
import uk.ac.warwick.tabula.commands.groups.admin.ListAllSmallGroupSetsCommand
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(Array("/v1/groups"))
class AllSmallGroupSetsController extends ApiController
	with ListAllSmallGroupSetsApi
	with SmallGroupSetToJsonConverter
	with SmallGroupToJsonConverter
	with SmallGroupEventToJsonConverter
	with AssessmentMembershipInfoToJsonConverter
	with AutowiringSmallGroupServiceComponent

trait ListAllSmallGroupSetsApi {
	self: ApiController with SmallGroupSetToJsonConverter with SmallGroupServiceComponent =>

	@ModelAttribute("listCommand")
	def command(): ListAllSmallGroupSetsCommand.Command = ListAllSmallGroupSetsCommand()

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@ModelAttribute("listCommand") command: ListAllSmallGroupSetsCommand.Command, errors: Errors): Mav =
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val info = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"total" -> info.total,
				"results" -> info.results,
				"num" -> info.maxResults,
				"skip" -> info.firstResult,
				"academicYear" -> info.academicYear.toString,
				"groups" -> info.sets.map(jsonSmallGroupSetObject)
			)))
		}

}
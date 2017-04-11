package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings.ScheduledMeetingRecordMissedCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.ScheduledMeetingRecord
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/missed"))
class ScheduledMeetingRecordMissedController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def getCommand(@PathVariable meetingRecord: ScheduledMeetingRecord) =
		ScheduledMeetingRecordMissedCommand(mandatory(meetingRecord))


	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable meetingRecord: ScheduledMeetingRecord
	): Mav = {

		if (!errors.hasErrors) {
			command.apply()
			val resultMap = Map(
				"status" -> "successful"
			)
			Mav(new JSONView(resultMap))
		} else {
			val additionalData = Map("formId" -> "meeting-%s".format(meetingRecord.id))
			Mav(new JSONErrorView(errors, additionalData))
		}

	}
}

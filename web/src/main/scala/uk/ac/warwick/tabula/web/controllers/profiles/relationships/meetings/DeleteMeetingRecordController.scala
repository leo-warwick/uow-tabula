package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings._
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.services.AutowiringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

abstract class AbstractRemoveMeetingRecordController extends ProfilesController {

	def doApply(command: Appliable[AbstractMeetingRecord], errors: Errors): Mav = {
		if (!errors.hasErrors) {
			command.apply()
			Mav(new JSONView(Map("status" -> "successful")))
		}
		else {
			Mav(new JSONErrorView(errors))
		}
	}
}

@Controller
@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/delete"))
class DeleteMeetingRecordController extends AbstractRemoveMeetingRecordController {

	validatesSelf[SelfValidating]

	@ModelAttribute("deleteMeetingRecordCommand")
	def getDeleteCommand(@PathVariable meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser): DeleteMeetingRecordCommand with ComposableCommand[AbstractMeetingRecord] with AutowiringMeetingRecordServiceComponent with DeleteMeetingRecordCommandValidation with RemoveMeetingRecordPermissions with RemoveMeetingRecordDescription with RemoveMeetingRecordState with DeleteScheduledMeetingRecordNotification = {
		DeleteMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def deleteMeetingRecord(@Valid @ModelAttribute("deleteMeetingRecordCommand") deleteCommand: Appliable[AbstractMeetingRecord],
			errors: Errors): Mav = {
		doApply(deleteCommand, errors)
	}

}

@Controller
@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/restore"))
class RestoreMeetingRecordController extends AbstractRemoveMeetingRecordController {

	showDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("restoreMeetingRecordCommand")
	def getRestoreCommand(@PathVariable meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser): RestoreMeetingRecordCommand with ComposableCommand[AbstractMeetingRecord] with AutowiringMeetingRecordServiceComponent with RestoreMeetingRecordCommandValidation with RemoveMeetingRecordPermissions with RemoveMeetingRecordDescription with RemoveMeetingRecordState with RestoreScheduledMeetingRecordNotification = {
		RestoreMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def restoreMeetingRecord(@Valid @ModelAttribute("restoreMeetingRecordCommand") restoreCommand: Appliable[AbstractMeetingRecord], errors: Errors): Mav = {
		doApply(restoreCommand, errors)
	}

}

@Controller
@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/purge"))
class PurgeMeetingRecordController extends AbstractRemoveMeetingRecordController {

	showDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("purgeMeetingRecordCommand")
	def getPurgeCommand(@PathVariable meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser): PurgeMeetingRecordCommand with ComposableCommand[AbstractMeetingRecord] with AutowiringMeetingRecordServiceComponent with RestoreMeetingRecordCommandValidation with RemoveMeetingRecordPermissions with RemoveMeetingRecordDescription with RemoveMeetingRecordState = {
		PurgeMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def purgeMeetingRecord(@Valid @ModelAttribute("purgeMeetingRecordCommand") purgeCommand: Appliable[AbstractMeetingRecord], errors: Errors): Mav = {
		doApply(purgeCommand, errors)
	}

}

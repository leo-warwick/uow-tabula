package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.groups.{AttendanceNoteAttachmentCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.{AbsenceType, Member}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{ProfileService, SmallGroupService}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/groups/note/{student}/{occurrence}"))
class GroupsAttendanceNoteController extends GroupsController {

	@Autowired var smallGroupService: SmallGroupService = _
	@Autowired var profileService: ProfileService = _

	@RequestMapping
	def home(
		@PathVariable("student") member: Member,
		@PathVariable occurrence: SmallGroupEventOccurrence
	): Mav = {
		val attendanceNote = smallGroupService.getAttendanceNote(member.universityId, occurrence).getOrElse(throw new ItemNotFoundException())
		val attendance = smallGroupService.getAttendance(member.universityId, occurrence).orNull
		Mav("groups/attendance/view_note",
			"attendanceNote" -> attendanceNote,
			"attendance" -> attendance,
			"updatedBy" -> profileService.getAllMembersWithUserId(attendanceNote.updatedBy).headOption.map{_.fullName}.getOrElse(""),
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}

}

@Controller
@RequestMapping(Array("/groups/note/{student}/{occurrence}/attachment/{fileName}"))
class GroupsAttendanceNoteAttachmentController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable("student") member: Member, @PathVariable occurrence: SmallGroupEventOccurrence): Appliable[Option[RenderableFile]] =
		AttendanceNoteAttachmentCommand(mandatory(member), mandatory(occurrence), user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: Appliable[Option[RenderableFile]]): RenderableFile = {
		cmd.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/groups/note/{student}/{occurrence}/edit"))
class EditGroupsAttendanceNoteController extends GroupsController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable("student") member: Member,
		@PathVariable occurrence: SmallGroupEventOccurrence,
		@RequestParam(value="state", required=false) state: String
	) =
		EditAttendanceNoteCommand(member, occurrence, user, Option(state))

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm
	): Mav = {
		cmd.populate()
		form(cmd, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		@PathVariable("student") member: Member
	): Mav = {
		cmd.populate()
		form(cmd)
	}

	private def form(
		cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		isIframe: Boolean = false
	) = {
		val mav = Mav("groups/attendance/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"returnTo" -> getReturnTo(Routes.tutor.mygroups),
			"isModal" -> ajax,
			"isIframe" -> isIframe
		)
		if(ajax)
			mav.noLayout()
		else if (isIframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("isIframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, isIframe = true)
		} else {
			cmd.apply()
			Mav("groups/attendance/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		errors: Errors
	): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.tutor.mygroups)
		}
	}

}

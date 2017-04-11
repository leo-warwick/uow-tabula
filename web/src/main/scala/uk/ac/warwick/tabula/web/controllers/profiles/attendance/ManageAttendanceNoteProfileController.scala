package uk.ac.warwick.tabula.web.controllers.profiles.attendance

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.commands.attendance.note.{AttendanceNoteAttachmentCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{AbsenceType, StudentMember}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController


@Controller
@RequestMapping(Array("/profiles/attendance/note/{student}/{point}"))
class AttendanceNoteProfileController extends AbstractViewProfileController {

	@Autowired var monitoringPointService: AttendanceMonitoringService = _
	@Autowired var userLookup: UserLookupService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint
	): Mav = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, point).getOrElse(throw new ItemNotFoundException())

		val mav = Mav("profiles/attendance/view_note",
			"attendanceNote" -> attendanceNote,
			"updatedBy" -> userLookup.getUserByUserId(attendanceNote.updatedBy).getFullName,
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		)

		val pointsCheckpointsMap = monitoringPointService.getCheckpoints(Seq(point), student)
		if (pointsCheckpointsMap.nonEmpty) mav.addObjects("checkpoint" -> pointsCheckpointsMap.head._2)
		mav.noLayoutIf(ajax)
	}

}

@Controller
@RequestMapping(Array("/profiles/attendance/note/{student}/{point}/attachment/{fileName}"))
class AttendanceNoteProfileAttachmentController extends ProfilesController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Appliable[Option[RenderableFile]] =
		AttendanceNoteAttachmentCommand(mandatory(student), mandatory(point), user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: Appliable[Option[RenderableFile]]): RenderableFile = {
		cmd.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/profiles/attendance/note/{student}/{point}/edit"))
class EditAttendanceNoteProfileController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@RequestParam(value="state", required=false) state: String
	) =
		EditAttendanceNoteCommand(student, point, user, Option(state))

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	): Mav = {
		cmd.populate()
		form(cmd, student, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	): Mav = {
		cmd.populate()
		form(cmd, student)
	}

	private def form(
		cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		student: StudentMember,
		isIframe: Boolean = false
	) = {
		val mav = Mav("profiles/attendance/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
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
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, student, isIframe = true)
		} else {
			cmd.apply()
			Mav("profiles/attendance/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, student)
		} else {
			cmd.apply()
			Redirect(Routes.Profile.attendance(student), "success" -> true).noNavigation()
		}
	}

}


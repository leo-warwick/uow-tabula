package uk.ac.warwick.tabula.web.controllers.attendance

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.note.{AttendanceNoteAttachmentCommand, BulkAttendanceNoteCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{AbsenceType, StudentMember}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/note/{academicYear}/{student}/{point}"))
class AttendanceNoteController extends AttendanceController {

	@Autowired var monitoringPointService: AttendanceMonitoringService = _
	@Autowired var userLookup: UserLookupService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, point).getOrElse(throw new ItemNotFoundException())

		val mav = Mav("attendance/note/view_note",
			"attendanceNote" -> attendanceNote,
			"academicYear" -> academicYear.startYear.toString,
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
@RequestMapping(Array("/attendance/note/{academicYear}/{student}/{point}/attachment/{fileName}"))
class AttendanceNoteAttachmentController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint): Appliable[Option[RenderableFile]] =
		AttendanceNoteAttachmentCommand(mandatory(student), mandatory(point), user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: Appliable[Option[RenderableFile]]): RenderableFile = {
		cmd.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/attendance/note/{academicYear}/{student}/{point}/edit"))
class EditAttendanceNoteController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="state", required=false) state: String
	) =
		EditAttendanceNoteCommand(student, point, user, Option(state))

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		cmd.populate()
		form(cmd, student, academicYear, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		cmd.populate()
		form(cmd, student, academicYear)
	}

	private def form(
		cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		student: StudentMember,
		academicYear: AcademicYear,
		isIframe: Boolean = false
	) = {
		val mav = Mav("attendance/note/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"returnTo" -> getReturnTo(Routes.View.student(student.homeDepartment, academicYear, student)),
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
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, student, academicYear, isIframe = true)
		} else {
			cmd.apply()
			Mav("attendance/note/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, student, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.View.student(student.homeDepartment, academicYear, student))
		}
	}

}

@Controller
@RequestMapping(Array("/attendance/note/{academicYear}/bulk/{point}/edit"))
class BulkEditAttendanceNoteController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint,
		@RequestParam students: JList[StudentMember]
	) = BulkAttendanceNoteCommand(point, students.asScala, user)


	private def form(
		cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		academicYear: AcademicYear,
		isAuto: Boolean = false
	) = {
		val mav = Mav("attendance/note/bulk_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"isModal" -> ajax,
			"isAuto" -> isAuto
		)
		if(ajax)
			mav.noLayout()
		else
			mav.noNavigation()
	}

	@RequestMapping(method=Array(GET, HEAD, POST))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint,
		@RequestParam(value="isAuto", required=false) isAuto: Boolean
	): Mav = {
		cmd.populate()
		form(cmd, academicYear, isAuto)
	}

	@RequestMapping(method=Array(GET, HEAD, POST), params=Array("isSave"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		errors: Errors,
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, academicYear)
		} else {
			cmd.apply()
			Mav("attendance/note/bulk_note", "success" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

}